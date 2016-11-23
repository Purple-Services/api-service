(ns api.handler
  (:require [common.util :refer [! unless-p ver< coerce-double log-error
                                 only-prod-or-dev rand-str-alpha-num]]
            [common.db :refer [conn]]
            [common.config :as config]
            [common.coupons :refer [format-coupon-code]]
            [common.orders :refer [cancel]]
            [common.users :refer [details send-feedback valid-session?]]
            [api.pages :as pages]
            [api.auth :as auth]
            [api.dispatch :as dispatch]
            [api.orders :as orders]
            [clojure.walk :refer [keywordize-keys]]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.json :as middleware]
            [ring.util.response :refer [header response redirect]]
            [ring.middleware.ssl :refer [wrap-ssl-redirect]]
            [clojure.string :as s]))

(defn wrap-page [resp]
  (header resp "Content-Type" "text/html; charset=utf-8"))

(defn wrap-xml [resp]
  (header resp "Content-Type" "text/xml; charset=utf-8"))

(defn wrap-force-ssl [resp]
  (if config/has-ssl?
    (wrap-ssl-redirect resp)
    resp))

(defn with-auth
  [db-conn req f]
  (let [[api-key user-auth-token] (auth/get-user-and-pass (:headers req))]
    (if (auth/valid-api-key? db-conn api-key)
      (if-let [user-id (auth/user-auth-token->user-id db-conn user-auth-token)]
        (f user-id)
        {:success false
         :message "Invalid User Auth Token."})
      {:success false
       :message "Invalid API Key."})))

(defroutes app-routes
  (context "/v1" []
           (context "/orders" []
                    (wrap-force-ssl
                     (defroutes orders-routes
                       (POST "/request" req
                             (response
                              (let [params (keywordize-keys (:body req))
                                    db-conn (conn)]
                                (with-auth db-conn req
                                  (fn [user-id]
                                    (orders/request
                                     db-conn
                                     user-id
                                     (:lat params)
                                     (:lng params)
                                     (:vehicle_id params)
                                     (Integer. (:time_limit params))
                                     (if (= "fillup" (:gallons params))
                                       (:gallons params)
                                       (Integer. (:gallons params)))
                                     (Integer. (:gas_price params))
                                     (Integer. (:delivery_fee params))
                                     (:special_instructions params)))))))
                       (GET "/get" req
                            (response
                             (let [params (keywordize-keys (:params req))
                                   db-conn (conn)]
                               (with-auth db-conn req
                                 (fn [user-id]
                                   (orders/get-by-user
                                    db-conn
                                    user-id
                                    (:vehicle_id params)
                                    (if (= "asc" (:sort params))
                                      "asc"
                                      "desc")
                                    (if (s/blank? (:start params))
                                      0
                                      (Integer. (:start params)))
                                    (if (s/blank? (:limit params))
                                      50
                                      (Integer. (:limit params))))))))))))
           (wrap-force-ssl
            (defroutes availability-routes
              (GET "/availability" req
                   (response
                    (let [params (keywordize-keys (:params req))
                          db-conn (conn)]
                      (with-auth db-conn req
                        (fn [user-id]
                          (dispatch/availability db-conn
                                                 user-id
                                                 (:lat params)
                                                 (:lng params)
                                                 (:vehicle_id params))))))))))
  ;; just a helpful utility
  (GET "/gen-id" [] (response {:success true
                               :id (rand-str-alpha-num 20)}))
  (GET "/docs" [] (wrap-page (response (pages/docs))))
  (GET "/ok" [] (response {:success true}))
  (GET "/" [] (redirect "/docs"))
  (route/resources "/")
  (route/not-found (wrap-page (response (pages/not-found-page)))))

(def app
  (-> (handler/site app-routes)
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :put :post :delete])
      (middleware/wrap-json-body)
      (middleware/wrap-json-response)))
