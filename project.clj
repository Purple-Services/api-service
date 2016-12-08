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
                       :env {:env "local"
                             :aws-access-key-id "AKIAJLB35GOFQUJZCX5A"
                             :aws-secret-key "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"
                             :db-host "localhost"
                             :db-name "ebdb"
                             :db-port "3307"
                             :db-user "root"
                             :db-password ""
                             :sendgrid-api-key "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"
                             :email-user "no-reply@purpledelivery.com"
                             :email-password "HJdhj34HJd"
                             :stripe-private-key "sk_test_6Nbxf0bpbBod335kK11SFGw3"
                             :sift-science-api-key "bfd841741209c423"
                             :sns-app-arn-apns "arn:aws:sns:us-west-2:336714665684:app/APNS_SANDBOX/Purple"
                             :sns-app-arn-gcm  "arn:aws:sns:us-west-2:336714665684:app/GCM/Purple"
                             :twilio-account-sid "AC0a0954acca9ba8c527f628a3bfaf1329"
                             :twilio-auto-token "3da1b036da5fb7716a95008c318ff154"
                             :twilio-form-number "+13239243338"
                             :segment-write-key "dSGXoQZwDaba6VflT12c0LbBZJaNJdtP"
                             :base-url "http://localhost:3000/"
                             :has-ssl "NO"
                             :basic-auth-username "purpleadmin"
                             :basic-auth-password "gasdelivery8791"
                             :basic-auth-read-only-username "readonly"
                             :basic-auth-read-only-password "viewpurplestats"
                             :basic-auth-courier-manager-username "courier"
                             :basic-auth-courier-manager-password "emptytank"
                             :sift-webhook-auth-username "siftwebhook"
                             :sift-webhook-auth-password "Hhh78hhddk988"
                             :wiw-key "5876b896f67489deb99592e46c4d5bc45d55ce10"
                             :wiw-token "e125c97e9affe18742f83b39cc48b1500a722eca"
                             :dashboard-google-browser-api-key "AIzaSyCdw2r0sd285l1GC7b0h9nCQlq8GaKJrnI"
                             :auto-assign-google-server-api-key "AIzaSyAXQtxXwmClUqbEw8mDjOHsufHVw7G0Sbs"
                             :api-google-server-api-key "AIzaSyAJzGgrSoctqm-LzVwYFn3jqejBtVBOv0E"
                             :google-oauth-web-client-id "727391770434-33032g56t6p38uh9sj9dqtst53eipaka.apps.googleusercontent.com"
                             :courier-app-download-url-iphone ""
                             :courier-app-download-url-android ""
                             :test-db-host "localhost"
                             :test-db-name "ebdb_test"
                             :test-db-port "3307"
                             :test-db-user "root"
                             :test-db-password ""}}]}
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :aws {:beanstalk {:app-name "api"
                    :environments [{:name "api-prod"}
                                   {:name "api-dev"}]
                    :s3-bucket "leinbeanstalkpurple"
                    :region "us-west-2"}})
