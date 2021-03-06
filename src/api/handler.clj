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
            [api.vehicles :as vehicles]
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

(defmacro demand-ssl
  [req & body]
  `(if (and ~config/has-ssl? (not= :https (:scheme ~req)))
     {:success false
      :message "Please use https protocol."
      :code "https-required"}
     (do ~@body)))

(defn with-auth
  [db-conn req f]
  (let [[api-key user-auth-token] (auth/get-user-and-pass (:headers req))]
    (if (auth/valid-api-key? db-conn api-key)
      (if-let [user-id (auth/user-auth-token->user-id db-conn user-auth-token)]
        (f user-id)
        {:success false
         :message "Invalid User Auth Token."
         :code "invalid-user-auth-token"})
      {:success false
       :message "Invalid API Key."
       :code "invalid-api-key"})))

(defroutes app-routes
  (context "/v1" []
           (defroutes availability-routes
             (GET "/availability" req
                  (response
                   (demand-ssl
                    req
                    (let [params (keywordize-keys (:params req))
                          db-conn (conn)]
                      (with-auth db-conn req
                        (fn [user-id]
                          (dispatch/availability db-conn
                                                 user-id
                                                 (:lat params)
                                                 (:lng params)
                                                 (:vehicle_id params)))))))))
           (context "/orders" []
                    (defroutes orders-routes
                      (POST "/request" req
                            (response
                             (demand-ssl
                              req
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
                                       (coerce-double (:gallons params)))
                                     (Integer. (:gas_price params))
                                     (Integer. (:delivery_fee params))
                                     (:special_instructions params)
                                     (:street_address params))))))))
                      (GET "/get" req
                           (response
                            (demand-ssl
                             req
                             (let [params (keywordize-keys (:params req))
                                   db-conn (conn)]
                               (with-auth db-conn req
                                 (fn [user-id]
                                   (orders/get-by-user
                                    db-conn
                                    user-id
                                    (:vehicle_id params)
                                    (if-not (s/blank? (:sort params))
                                      (:sort params)
                                      "desc")
                                    (if (s/blank? (:start params))
                                      0
                                      (Integer. (:start params)))
                                    (if (s/blank? (:limit params))
                                      50
                                      (Integer. (:limit params))))))))))
                      (POST "/cancel/:id" [id :as req]
                            (response
                             (demand-ssl
                              req
                              (let [db-conn (conn)]
                                (with-auth db-conn req
                                  (fn [user-id]
                                    (orders/cancel db-conn user-id id)))))))))
           (context "/vehicles" []
                    (defroutes vehicles-routes
                      (GET "/get" req
                           (response
                            (demand-ssl
                             req
                             (let [params (keywordize-keys (:params req))
                                   db-conn (conn)]
                               (with-auth db-conn req
                                 (fn [user-id]
                                   (vehicles/get-by-user db-conn user-id))))))))))
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
