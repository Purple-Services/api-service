(ns api.test.handler
  (:use cheshire.core)
  (:require [clojure.test :refer :all]
            [common.db :refer [conn]]
            [api.handler :refer [app]]
            [api.test.db-tools :refer [setup-ebdb-test-for-conn-fixture
                                       ebdb-test-config]]
            [ring.mock.request :as mock]))

(use-fixtures :once setup-ebdb-test-for-conn-fixture)

(deftest test-app
  (testing "not-found route"
    (let [response (app (mock/request :get "/i-n-v-a-l-i-d"))]
      (is (= 404
             (:status response)))))

  (testing "ok route (for server monitoring)"
    (let [response (app (mock/request :get "/ok"))]
      (is (:success (parse-string (:body response) true)))))
  
  (testing "root route redirects to docs page"
    (let [response (app (mock/request :get "/"))]
      (is (= 302
             (:status response))))))

;; (deftest test-user-interactions
;;   (testing "A user can update their number with a good 10 digit phone number"
;;     (let [post-data {:user_id user-id
;;                      :token token
;;                      :version "1.5.0"
;;                      :user {:phone_number "800-555-1212"
;;                             :name "Test User"}}
;;           response (app (->  (mock/request :post "/user/edit"
;;                                            (generate-string post-data))
;;                              (mock/content-type "application/json")))
;;           body (parse-string (:body response) true)]
;;       (is (:success body)))))

