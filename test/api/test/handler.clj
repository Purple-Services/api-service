(ns api.test.handler
  (:use cheshire.core)
  (:require [clojure.test :refer :all]
            [common.db :refer [conn]]
            [api.handler :refer [app]]
            [api.test.db-tools :refer [setup-ebdb-test-for-conn-fixture
                                       ebdb-test-config]]
            [ring.mock.request :as mock]))

(use-fixtures :once setup-ebdb-test-for-conn-fixture)

(deftest test-routes
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
             (:status response)))))
  
  (testing "availability"
    (let [params {:lat "33.995632"
                  :lng "-118.474990"
                  :vehicle_id "4clhMV0ewUMDB7O4460R"}
          response (app (->  (mock/request :get "/v1/availability")
                             (mock/query-string params)
                             (mock/header "Authorization" "Basic S0pQVzFiR25kRExFV1d1NUxwNUtKQWpndk1LS1NiSkE6c3FYd1RpaFZnVG9YMENkeTY1OW1DVksxZ1B6RjBBMThFR0VwSnRZbEdLQVVOa2dPR09zMnU3dE5UcUk2TGx0Q1VVWWhFdWJjdVQ1SWxQOFF0VFdLT0FLRkVYbTlWYlhWS1lWUmJoeTlTaWk5N3FqS2tsZ2JEa0NZMHY0UXF0Zk4=")
                             (mock/content-type "application/json")))
          body (parse-string (:body response) true)]
      (is (= {:success true,
              :availability
              {:time_choices
               [{:fee 599, :text "within 1 hour ($5.99)", :time 60}
                {:fee 399, :text "within 3 hours ($3.99)", :time 180}
                {:fee 299, :text "within 5 hours ($2.99)", :time 300}],
               :gallon_choices ["fill" 7.5 10 15],
               :octane "91",
               :gas_price 339,
               :tire_pressure_check_price 700}}
             body)))))

