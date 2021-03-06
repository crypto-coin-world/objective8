(ns objective8.front-end.templates.project-status
  (:require [net.cgrand.enlive-html :as html]
            [objective8.front-end.templates.template-functions :as tf]
            [objective8.front-end.templates.page-furniture :as f]))

(def project-status-template (html/html-resource "templates/jade/project-status.html"))

(defn project-status-page [{:keys [doc] :as context}]
  (html/at project-status-template
              [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
              [:.clj-masthead-signed-out] (html/substitute (f/masthead context))
              [:.clj-status-bar] (html/substitute (f/status-flash-bar context))))
