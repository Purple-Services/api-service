(defproject api "0.1.0-SNAPSHOT"
  :description "Purple API web service."
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.1.8"]
                 [ring/ring-json "0.1.2"]
                 ;; json and related utilities
                 [cheshire "5.4.0"]
                 [clj-http "1.0.1"]
                 ;; Google API
                 [gapi "1.0.1"]
                 ;; templating
                 [enlive "1.1.5"]
                 ;; date/time utilities
                 [clj-time "0.8.0"]
                 ;; this will be used by clj-aws down below instead of its
                 ;; default aws version
                 [com.amazonaws/aws-java-sdk "1.9.24"]
                 [clj-aws "0.0.1-SNAPSHOT"]
                 [org.clojure/data.priority-map "0.0.6"]
                 [ring-cors "0.1.7"]
                 [ring/ring-ssl "0.2.1"]
                 [ring-basic-authentication "1.0.5"]
                 [org.clojure/algo.generic "0.1.2"]
                 [common "2.0.2-SNAPSHOT"]]
  :pedantic? false
  :plugins [[lein-ring "0.8.13"]
            [lein-beanstalk "0.2.7"]
            [lein-exec "0.3.5"]]
  :ring {:handler api.handler/app
         :auto-reload? true
         :auto-refresh? true
         :reload-paths ["src" "resources" "checkouts"]}
  :profiles {:shared [{:dependencies
                       [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]
                        [org.seleniumhq.selenium/selenium-java "2.47.1"]
                        [clj-webdriver "0.7.2"]
                        [ring "1.5.0"]
                        [pjstadig/humane-test-output "0.6.0"]]
                       :injections
                       [(require 'pjstadig.humane-test-output)
                        (pjstadig.humane-test-output/activate!)]}]
             :local [:shared :profiles/local]
             :dev [:shared :profiles/dev]
             :prod [:shared :profiles/prod]
             :travis [:shared
                      {:plugins [[lein-environ "1.1.0"]]
                       :env {:env "local" ; to make common.config#L5 happy
                             :stripe-private-key ~(System/getenv "STRIPE_PRIVATE_KEY")
                             :test-db-host "localhost"
                             :test-db-name "ebdb_test"
                             :test-db-port "3306"
                             :test-db-user "root"
                             :test-db-password ""}}]}
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :aws {:beanstalk {:app-name "api"
                    :environments [{:name "api-prod"}
                                   {:name "api-dev"}]
                    :s3-bucket "leinbeanstalkpurple"
                    :region "us-west-2"}})
