(ns objective8.templates.dashboard-annotations
  (:require [net.cgrand.enlive-html :as html]
            [cemerick.url :as url]
            [objective8.utils :as utils]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [objective8.templates.page-furniture :as pf]
            [objective8.templates.template-functions :as tf]))


(def dashboard-annotations-template (html/html-resource "templates/jade/annotations-dashboard.html"))

(def annotations-list-snippet (html/select dashboard-annotations-template [[:.clj-dashboard-annotation-section-list-item html/first-of-type]]))

(def comments-snippet (html/select annotations-list-snippet [[:.clj-dashboard-annotation-item html/first-of-type]]))

(def dashboard-annotations-no-annotations-snippet (html/select pf/library-html-resource
                                                         [:.clj-library-key--dashboard-no-annotation-item]))

(defn render-comment [comment]
  (html/at comments-snippet
    [:.clj-dashboard-annotation-text] (html/content (:comment comment))
    [:.clj-dashboard-annotation-author] (html/content (:username comment))
    [:.clj-dashboard-annotation-date] (html/content (utils/iso-time-string->pretty-time (:_created_at comment)))
    [:.clj-dashboard-comment-up-count] (html/content (str (get-in comment [:votes :up])))
    [:.clj-dashboard-comment-down-count] (html/content (str (get-in comment [:votes :down])))))

(defn comment-list [annotation]
  (let [comments (:comments annotation)]
    (html/at comments-snippet
      [:.clj-dashboard-annotation-item]
      (html/clone-for [comment comments]
        [:.clj-dashboard-annotation-item] (html/substitute (render-comment comment))))))

(def annotation-snippet (html/select dashboard-annotations-template [[:.clj-dashboard-annotation-section-list-item html/first-of-type]]))

(defn dashboard-annotations-no-annotations [{:keys [translations data] :as context}]
  (html/at dashboard-annotations-no-annotations-snippet
    [:.clj-dashboard-no-annotation-item] (html/content (translations :writer-dashboard/no-annotations-message))))

(defn render-annotation [context annotation]
  (html/at annotation-snippet
    [:.clj-dashboard-annotation-section] (html/html-content (utils/hiccup->html (:section annotation)))
    [:.clj-dashboard-annotation-section-list] (html/content (comment-list annotation))))

(defn annotation-list-items [{:keys [data] :as context}]
  (let [annotations (:annotations data)]
    (html/at annotations-list-snippet
             [:.clj-dashboard-annotation-section-list-item]
             (html/clone-for [annotation annotations]
               [:.clj-dashboard-annotation-section-list-item] (html/substitute (render-annotation context annotation))))))

(defn annotation-list [{:keys [data] :as context}]
  (let [annotations (:annotations data)]
    (if (empty? annotations)
      (dashboard-annotations-no-annotations context)
      (annotation-list-items context))))

(def dashboard-annotations-navigation-item-snippet (html/select dashboard-annotations-template
                                                             [[:.clj-dashboard-navigation-item html/first-of-type]]))

(defn draft-label [{:keys [translations] :as context} draft]
  (str (translations :dashboard-comments/draft-label-prefix) ": " (utils/iso-time-string->pretty-time (:_created_at draft))))

(defn draft->navigation-list-item [{:keys [translations] :as context} draft]
  {:label (draft-label context draft)
   :link-count (get-in draft [:meta :comments-count])
   :uri (:uri draft)})

(defn objective->navigation-list-item [{:keys [translations] :as context} objective]
  {:label (translations :dashboard-comments/objective-label)
   :link-count (get-in objective [:meta :comments-count])
   :uri (:uri objective)})

(defn navigation-list [{:keys [data translations] :as context}]
  (let [objective (:objective data)
        draft-nav-items (mapv (partial draft->navigation-list-item context) (:drafts data))
        navigation-list-items (cond-> draft-nav-items
                                (not (empty? draft-nav-items)) (update-in [0 :label]
                                                                 #(str % " (" (translations :dashboard-comments/latest-draft-label) ")")))
        selected-draft-uri (:selected-draft-uri data)
        dashboard-url (url/url (utils/path-for :fe/dashboard-annotations :id (:_id objective)))]
    (html/at dashboard-annotations-navigation-item-snippet
             [:.clj-dashboard-navigation-item]
             (html/clone-for [item navigation-list-items]
                             [:.clj-dashboard-navigation-item] (if (= selected-draft-uri (:uri item))
                                                                 (html/add-class "on")
                                                                 (html/remove-class "on"))
                             [:.clj-dashboard-navigation-item-label] (html/content (:label item))
                             [:.clj-dashboard-navigation-item-link-count] nil
                             [:.clj-dashboard-navigation-item-link]
                             (html/set-attr :href
                                            (str (assoc dashboard-url
                                                        :query {:selected (:uri item)}
                                                        :anchor "dashboard-content")))))))

(defn dashboard-annotations [{:keys [doc data] :as context}]
  (let [objective (:objective data)
        selected-comment-target-uri (:selected-draft-uri data)
        dashboard-url (url/url (utils/path-for :fe/dashboard-annotations :id (:_id objective)))
        comment-view-type (:comment-view-type data)]
    (apply str
      (html/emit*
        (tf/translate context
          (pf/add-google-analytics
            (html/at dashboard-annotations-template
              [:title] (html/content (:title doc))
              [(and (html/has :meta) (html/attr= :name "description"))] (html/set-attr "content" (:description doc))
              [:.clj-masthead-signed-out] (html/substitute (pf/masthead context))
              [:.clj-status-bar] (html/substitute (pf/status-flash-bar context))

              [:.clj-dashboard-title-link] (html/set-attr :href (url/url (utils/path-for :fe/objective :id (:_id objective))))
              [:.clj-dashboard-title-link] (html/content (:title objective))

              [:.clj-dashboard-stat-participant] nil
              [:.clj-dashboard-stat-starred-amount] (html/content (str (get-in objective [:meta :stars-count])))
              [:.clj-writer-dashboard-navigation-questions-link] (html/set-attr :href (utils/path-for :fe/dashboard-questions :id (:_id objective)))
              [:.clj-writer-dashboard-navigation-comments-link] (html/set-attr :href (utils/path-for :fe/dashboard-comments :id (:_id objective)))
              [:.clj-writer-dashboard-navigation-annotations-link] (html/set-attr :href (utils/path-for :fe/dashboard-annotations :id (:_id objective)))
              [:.clj-dashboard-navigation-list] (html/content (navigation-list context))

              [:.clj-dashboard-annotation-list] (html/content (annotation-list context))
              [:.clj-dashboard-filter-list] nil)))))))