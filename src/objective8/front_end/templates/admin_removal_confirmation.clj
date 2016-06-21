(ns objective8.front-end.templates.admin-removal-confirmation
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [objective8.front-end.templates.page-furniture :as pf]
            [objective8.front-end.templates.template-functions :as tf]))

(def admin-removal-confirmation-template (html/html-resource "templates/jade/admin-removal-confirmation.html" {:parser jsoup/parser}))

(defn admin-removal-confirmation-page [{:keys [anti-forgery-snippet doc data] :as context}]
  (let [{:keys [removal-uri removal-sample comment-on-uri] :as removal-data} (:removal-data data)
        is-a-comment? (and (.contains removal-uri "comments") (not (nil? comment-on-uri)))]
    (html/at admin-removal-confirmation-template
             [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr :content (:description doc))
             [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))
             [:.clj-objective-title] (html/content removal-sample)
             [:.clj-removal-confirmation-form] (html/prepend anti-forgery-snippet)
             [:.clj-removal-confirmation-form] (if is-a-comment? (html/set-attr :action "/meta/remove-comment-confirmation") identity)
             [:.clj-cancel-removal] (if is-a-comment? (html/set-attr :href comment-on-uri) identity)
             [:.clj-removal-uri] (html/set-attr :value removal-uri)
             [:.clj-comment-on-uri] (if (nil? comment-on-uri) (html/substitute nil) (html/set-attr :value comment-on-uri))

             )))
