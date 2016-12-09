(ns api.auth
  (:require [common.db :refer [conn !select !insert !update]]
            [clojure.string :as s]
            [clojure.data.codec.base64 :as base64]))

;; borrows from https://github.com/remvee/ring-basic-authentication

(defn- byte-transform
  "Used to encode and decode strings.  Returns nil when an exception
  was raised."
  [direction-fn string]
  (try
    (s/join (map char (direction-fn (.getBytes string))))
    (catch Exception _)))

(defn- encode-base64
  "Will do a base64 encoding of a string and return a string."
  [^String string]
  (byte-transform base64/encode string))

(defn- decode-base64
  "Will do a base64 decoding of a string and return a string."
  [^String string]
  (byte-transform base64/decode string))

(defn get-user-and-pass
  [headers]
  (let [auth (headers "authorization")
        cred (and auth (decode-base64 (last (re-find #"^Basic (.*)$" auth))))
        [user pass] (and cred (s/split (str cred) #":" 2))]
    [user pass]))

(defn valid-api-key?
  [db-conn api-key]
  (when-not (s/blank? api-key)
    (seq (!select db-conn
                  "api_keys"
                  [:id]
                  {:api_key api-key
                   :active 1}))))

(defn user-auth-token->user-id
  [db-conn user-auth-token]
  (when-not (s/blank? user-auth-token)
    (some-> (!select db-conn
                     "sessions"
                     [:user_id]
                     {:token user-auth-token
                      :source "api"
                      :active 1})
            first
            :user_id)))
