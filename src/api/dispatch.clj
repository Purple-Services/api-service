(ns api.dispatch
  (:require [common.db :refer [conn !select !insert !update]]
            [common.config :as config]
            [common.util :refer [! cents->dollars-str five-digit-zip-code
                                 get-event-time in? minute-of-day->hmma
                                 now-unix only-prod only-prod-or-dev
                                 segment-client send-sms split-on-comma
                                 unix->minute-of-day log-error catch-notify
                                 unix->day-of-week]]
            [common.orders :as orders]
            [common.users :as users]
            [common.zones :refer [get-zip-def is-open-now? order->zones]]
            [common.subscriptions :as subscriptions]
            [clojure.string :as s]))

(defn delivery-time-map
  "Build a map that describes a delivery time option for the mobile app."
  [time-str      ; e.g., "within 5 hours"
   delivery-fee  ; fee amount in cents
   num-free      ; number of free deliveries that subscription provides
   num-free-used ; number of free deliveries already used in this period
   sub-discount] ; the discount the subscription gives after all free used
  (let [fee-str #(if (= % 0) "free" (str "$" (cents->dollars-str %)))
        gen-text #(str time-str " (" % ")")]
    (if (not (nil? num-free)) ;; using a subscription?
      (let [num-free-left (- num-free num-free-used)]
        (if (pos? num-free-left)
          {:service_fee 0 ; the mobile app calls delivery fee "service_fee"
           :text (gen-text (if (< num-free-left 1000)
                             (str num-free-left " left")
                             (fee-str 0)))}
          (let [after-discount (max 0 (+ delivery-fee sub-discount))]
            {:service_fee after-discount
             :text (gen-text (fee-str after-discount))})))
      {:service_fee delivery-fee
       :text (gen-text (fee-str delivery-fee))})))

(defn delivery-times-map
  "Given subscription usage map and service fee, create the delivery-times map."
  [user zip-def sub delivery-fees]
  (let [has-free-three-hour? (pos? (or (:num_free_three_hour sub) 0))
        has-free-one-hour? (pos? (or (:num_free_one_hour sub) 0))]
    (->> (remove #(or (and (= 300 (val %))
                           ;; hide 5-hour option if using 1 or 3-hour sub
                           (or has-free-three-hour? has-free-one-hour?))
                      (and (= 180 (val %))
                           ;; hide 3-hour option if using 1-hour sub
                           (or has-free-one-hour?)))
                 ;; (:time-choices zip-def) ; still show all three options always
                 ;; temporarily hardcoding this in
                 {:0 60
                  :1 180
                  :2 300})
         (#(for [[k v] %
                 :let [[num-as-word time-str]
                       (case v
                         300 ["five" "within 5 hours"]
                         180 ["three" "within 3 hours"]
                         60  ["one" "within 1 hour"])]]
             [v (assoc (delivery-time-map time-str
                                          (get delivery-fees v)
                                          (((comp keyword str)
                                            "num_free_" num-as-word "_hour") sub)
                                          (((comp keyword str)
                                            "num_free_" num-as-word "_hour_used") sub)
                                          (((comp keyword str)
                                            "discount_" num-as-word "_hour") sub))
                       :order (Integer. (name k)))]))
         (into {}))))

(defn available
  [user zip-def subscription octane]
  {:octane octane
   :gallon_choices
   (cond
     (users/is-managed-account? user) [7.5 10 15 20 25 30]
     (in? [1 2] (:id subscription)) [7.5 10 15 20]
     :else (vals (:gallon-choices zip-def)))
   :price_per_gallon (get (:gas-price zip-def) octane)
   :times (->> (:delivery-fee zip-def)
               (delivery-times-map user zip-def subscription)
               (into {}))
   :tire_pressure_check_price (:tire-pressure-price zip-def)})

(defn availabilities-map
  [db-conn zip-code user subscription]
  (if-let [zip-def (get-zip-def db-conn zip-code)]
    (if (is-open-now? zip-def)
      {:success true
       ;; todo - get vehicle octane
       :availabilities (available user zip-def subscription "87")}
      {:success true
       :availabilities []
       :unavailable-reason (:closed-message zip-def)})
    ;; We don't service this ZIP code at all.
    {:success true
     :availabilities []
     :unavailable-reason
     (str "Sorry, we are unable to deliver gas to your "
          "location. We are rapidly expanding our service "
          "area and hope to offer service to your "
          "location very soon.")}))

(defn latlng->zip
  "Get 5-digit ZIP given lat lng."
  [lat lng]
  "90025")

(defn availability
  "Get an availability map to tell client what orders it can offer to user."
  [db-conn user-id lat lng vehicle-id]
  (let [user (users/get-user-by-id db-conn user-id)
        subscription (when (subscriptions/valid? user)
                       (subscriptions/get-with-usage db-conn user))
        zip-code (latlng->zip lat lng)]
    (merge {:success true}
           ;; this will give us :availabilities & :unavailable-reason
           (availabilities-map db-conn zip-code user subscription))))
