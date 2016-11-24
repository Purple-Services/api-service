(ns api.vehicles
  (:require [common.config :as config]
            [common.db :refer [!select !insert mysql-escape-str]]
            [common.util :refer [convert-timestamp]]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as s]))

(defn clean-vehicle
  [o]
  (let [safe-keys [:id :year :make :model :color :gas_type :license_plate :vin
                   :timestamp_created]
        key-old->new {:gas_type :octane}]
    (-> o
        (select-keys safe-keys)
        (rename-keys key-old->new)
        convert-timestamp)))

(defn get-by-user
  "Gets all a user's vehicles."
  [db-conn user-id]
  {:success true
   :vehicles (into []
                   (map clean-vehicle
                        (!select db-conn
                                 "vehicles"
                                 ["*"]
                                 {:user_id user-id
                                  :active 1}
                                 :append "ORDER BY timestamp_created DESC")))})
