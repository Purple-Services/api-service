(ns api.handler
  (:require [common.util :refer [! unless-p ver< coerce-double
                                 log-error only-prod-or-dev]]
            [common.db :refer [conn]]
            [common.config :as config]
            [common.coupons :refer [format-coupon-code]]
            [common.orders :refer [cancel]]
            [common.users :refer [details send-feedback valid-session?]]
            [api.pages :as pages]
            [api.auth :as auth]
            [api.dispatch :as dispatch]
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
                                  {:success false
                                   :message "This feature is not yet implemented."}
                                  ;;  (orders/add db-conn
                                  ;;              (:user_id b)
                                  ;;              (:order b)
                                  ;;              :bypass-zip-code-check
                                  ;;              (ver< (or (:version b) "0")
                                  ;;                    "1.2.2"))
                                  ))))
                       ;; Customer tries to cancel order
                       (GET "/get" req
                            (response
                             (let [params (keywordize-keys (:body req))
                                   db-conn (conn)]
                               (with-auth db-conn req
                                 {:success false
                                  :message "This feature is not yet implemented."}
                                 ;;  (cancel db-conn
                                 ;;          (:user_id b)
                                 ;;          (:order_id b))
                                 )))))))
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
