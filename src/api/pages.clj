(ns api.pages
  (:require [common.config :as config]
            [clojure.string :as s]
            [net.cgrand.enlive-html :refer [content deftemplate set-attr
                                            replace-vars transform-content]]))

(deftemplate index-template "templates/index.html"
  [x]
  [:title] (content (:title x))
  [:#heading] (content (:heading x))
  [:#intro] (content (:intro x)))

(defn not-found-page []
  (s/join (index-template {:heading "Page Not Found"
                           :intro "Sorry, that page could not be found."})))

(deftemplate docs-template "templates/docs.html"
  [x]
  [:title] (content (:title x))
  [:#main-heading] (content (:title x))
  [:code] (transform-content (replace-vars {:base-url config/base-url})))

(defn docs []
  (s/join (docs-template {:title (if (.contains config/base-url "sandbox")
                                   "Purple Sandbox API Docs"
                                   "Purple API Docs")})))


