(ns api.pages
  (:require
   [net.cgrand.enlive-html :refer [content deftemplate set-attr]]
   [common.config :as config]))

(deftemplate index-template "templates/index.html"
  [x]
  [:title] (content (:title x))
  [:#heading] (content (:heading x))
  [:#intro] (content (:intro x)))

(defn not-found-page []
  (apply str (index-template {:heading "Page Not Found"
                              :intro "Sorry, that page could not be found."})))

(deftemplate docs-template "templates/docs.html"
  [x]
  [:title] (content (:title x)))

(defn docs []
  (apply str (docs-template {:title "Purple API Docs"})))
