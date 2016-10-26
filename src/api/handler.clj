(ns api.handler
  (:require [common.util :refer [! unless-p ver< coerce-double
                                 log-error only-prod-or-dev]]
            [common.db :refer [conn]]
            [common.config :as config]
            [common.coupons :refer [format-coupon-code]]
            [common.orders :refer [cancel]]
            [common.users :refer [details send-feedback valid-session?]]
            [api.pages :as pages]
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

(defmacro demand-user-auth
  [db-conn user-id token & body]
  `(if (valid-session? ~db-conn ~user-id ~token)
     (do ~@body)
     {:success false
      :message "Something's wrong. Please log out and log back in."}))

(defroutes app-routes
  ;; (context "/orders" []
  ;;          (wrap-force-ssl
  ;;           (defroutes orders-routes
  ;;             (POST "/add" {body :body}
  ;;                   (response
  ;;                    (let [b (keywordize-keys body)
  ;;                          db-conn (conn)]
  ;;                      (demand-user-auth
  ;;                       db-conn
  ;;                       (:user_id b)
  ;;                       (:token b)
  ;;                       (orders/add db-conn
  ;;                                   (:user_id b)
  ;;                                   (:order b)
  ;;                                   :bypass-zip-code-check
  ;;                                   (ver< (or (:version b) "0")
  ;;                                         "1.2.2"))))))
  ;;             ;; Customer tries to cancel order
  ;;             (POST "/cancel" {body :body}
  ;;                   (response
  ;;                    (let [b (keywordize-keys body)
  ;;                          db-conn (conn)]
  ;;                      (demand-user-auth
  ;;                       db-conn
  ;;                       (:user_id b)
  ;;                       (:token b)
  ;;                       (cancel db-conn
  ;;                               (:user_id b)
  ;;                               (:order_id b)))))))))
  
  ;; (wrap-force-ssl
  ;;  ;; Check availability options for given params (location, etc.)
  ;;  (POST "/availability" {body :body}
  ;;        (response
  ;;         (let [b (keywordize-keys body)
  ;;               db-conn (conn)]
  ;;           (demand-user-auth
  ;;            db-conn
  ;;            (:user_id b)
  ;;            (:token b)
  ;;            (dispatch/availability db-conn
  ;;                                   (:zip_code b)
  ;;                                   (:user_id b)))))))
  
  
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
