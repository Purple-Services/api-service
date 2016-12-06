(ns api.dispatch
  (:require [common.db :refer [conn !select !insert !update]]
            [common.util :refer [cents->dollars-str in? catch-notify
                                 reverse-geocode]]
            [common.users :as users]
            [common.zones :refer [get-zip-def is-open-now? order->zones]]
            [common.subscriptions :as subscriptions]
            [clojure.string :as s]))

(defn delivery-time-map
  "Build a map that describes all the delivery time options available."
  [time-str      ; e.g., "within 5 hours"
   delivery-fee  ; fee amount in cents
   num-free      ; number of free deliveries that subscription provides
   num-free-used ; number of free deliveries already used in this period
   sub-discount] ; the discount the subscription gives after all free used
  (let [fee-str #(if (= % 0) "free" (str "$" (cents->dollars-str %)))
        gen-text #(str time-str " (" % ")")]
    (if (not (nil? num-free))
      ;; is using a subscription
      (let [num-free-left (- num-free num-free-used)]
        (if (pos? num-free-left)
          ;; they have some free deliveries left on their subscription
          {:fee 0
           :text (gen-text (if (< num-free-left 1000)
                             (str num-free-left " left")
                             (fee-str 0)))}
          ;; they're out of free deliveries, but maybe they get a discount?
          (let [after-discount (max 0 (+ delivery-fee sub-discount))]
            {:fee after-discount
             :text (gen-text (fee-str after-discount))})))
      ;; is not using a subscription
      {:fee delivery-fee
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
                 (:time-choices zip-def))
         (#(for [[k v] %
                 :let [[num-as-word time-str]
                       (case v
                         300 ["five" "within 5 hours"]
                         180 ["three" "within 3 hours"]
                         60  ["one" "within 1 hour"])]]
             (assoc (delivery-time-map
                     time-str
                     (get delivery-fees v)
                     (((comp keyword str) "num_free_" num-as-word "_hour")
                      sub)
                     (((comp keyword str) "num_free_" num-as-word "_hour_used")
                      sub)
                     (((comp keyword str) "discount_" num-as-word "_hour")
                      sub))
                    :time v))))))

(defn available
  [user zip-def subscription octane]
  {:time_limit_choices (delivery-times-map user zip-def subscription (:delivery-fee zip-def))
   :gallon_choices (conj (vals (:gallon-choices zip-def)) "fillup")
   :octane octane
   :gas_price (get (:gas-price zip-def) octane)
   :tire_pressure_fillup_price (:tire-pressure-price zip-def)})

(defn availability
  "Get an availability map to tell client what orders it can offer to user."
  [db-conn user-id lat lng vehicle-id]
  (let [user (users/get-user-by-id db-conn user-id)
        subscription (when (subscriptions/valid? user)
                       (subscriptions/get-with-usage db-conn user))
        vehicle (first (!select db-conn "vehicles" ["gas_type"]
                                {:user_id user-id ; security
                                 :id vehicle-id}))
        zip-code (:zip (reverse-geocode lat lng))]
    (if vehicle
      (if-let [zip-def (when zip-code (get-zip-def db-conn zip-code))]
        (if (is-open-now? zip-def)
          {:success true
           ;; todo - get vehicle octane
           :availability (available user zip-def subscription (:gas_type vehicle))}
          {:success false
           :message (:closed-message zip-def)})
        ;; We don't service this ZIP code at all.
        {:success false
         :message (str "Sorry, we are unable to deliver gas to your "
                       "location. We are rapidly expanding our service "
                       "area and hope to offer service to your "
                       "location very soon.")})
      {:success false
       :message "Sorry, we don't recognize your vehicle information."})))
