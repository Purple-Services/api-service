(ns api.orders
  (:require [common.config :as config]
            [common.db :refer [!select !insert mysql-escape-str]]
            [common.orders :as orders]
            [common.util :refer [convert-timestamp reverse-geocode
                                 compute-total-price]]
            [bouncer.core :as bouncer]
            [bouncer.validators :as v]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as s]))

(defn clean-order
  [o]
  (let [safe-keys [:id :tire_pressure_check :service_fee :target_time_start
                   :target_time_end :vehicle_id :total_price :paid
                   :special_instructions :status :gas_price :lat :address_zip
                   :lng :gallons :address_street :license_plate :gas_type
                   :timestamp_created]
        key-old->new {:tire_pressure_check :tire_pressure_fillup
                      :service_fee :delivery_fee
                      :target_time_start :time_window_start
                      :target_time_end :time_window_end
                      :gas_type :octane}]
    (-> o
        (select-keys safe-keys)
        (rename-keys key-old->new)
        convert-timestamp)))

(defn get-by-user
  "Gets a user's orders."
  [db-conn user-id vehicle-id sort start limit]
  (if-let [input-errors (first
                         (bouncer/validate
                          {:sort sort
                           :start start
                           :limit limit}
                          :sort [[v/member #{"asc" "desc"}]]
                          :start [v/integer [v/in-range [0 999999999]]]
                          :limit [v/integer [v/in-range [0 999999999]]]))]
    {:success false
     :message (str (s/join ". " (flatten (vals input-errors))) ".")
     :code "invalid-input"}
    {:success true
     :orders (into []
                   (map clean-order
                        (!select db-conn
                                 "orders"
                                 ["*"]
                                 (merge {:user_id user-id}
                                        (when vehicle-id {:vehicle_id vehicle-id}))
                                 :append (str "ORDER BY target_time_start " sort
                                              " LIMIT " start "," limit))))}))

(defn request
  [db-conn user-id lat lng vehicle-id time-limit gallons gas-price
   delivery-fee special-instructions street-address-override]
  (if-let [input-errors (first
                         (bouncer/validate
                          {:lat (try (Double/parseDouble (str lat))
                                     (catch Exception e 99999))
                           :lng (try (Double/parseDouble (str lng))
                                     (catch Exception e 99999))
                           :vehicle_id vehicle-id
                           :time_limit time-limit
                           :gallons gallons
                           :gas_price gas-price
                           :delivery_fee delivery-fee}
                          :lat [v/required [v/in-range [-180 180]]]
                          :lng [v/required [v/in-range [-180 180]]]
                          :vehicle_id [v/required]
                          :time_limit [v/required v/integer]
                          :gallons [v/required]
                          :gas_price [v/required v/integer]
                          :delivery_fee [v/required v/integer]))]
    {:success false
     :message (str (s/join ". " (flatten (vals input-errors))) ".")
     :code "invalid-input"}
    (if-let [geo-components (reverse-geocode lat lng)]
      (let [is-fillup? (= "fillup" gallons)]
        (orders/add
         db-conn
         user-id
         {:time time-limit
          :vehicle_id vehicle-id 
          :special_instructions (or special-instructions "")
          :lat lat
          :lng lng
          :address_street (:street geo-components)
          :address_zip (:zip geo-components)
          :is_fillup is-fillup?
          :gallons (if is-fillup? nil gallons)
          :gas_price gas-price
          :service_fee delivery-fee
          :total_price (if is-fillup?
                         7500 ; for fillups, auth $75
                         (compute-total-price gas-price
                                              gallons
                                              delivery-fee))}
         :street-address-override street-address-override))
      {:success false
       :message "Sorry, we were unable to determine your location."
       :code "invalid-latlng"})))

(defn cancel
  [db-conn user-id order-id]
  (select-keys (orders/cancel db-conn user-id order-id)
               [:success :message :code]))
