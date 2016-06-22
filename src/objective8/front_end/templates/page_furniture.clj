(ns objective8.front-end.templates.page-furniture
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            [cemerick.url :as url]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.front-end.permissions :as permissions]))

(def library-html "templates/jade/library.html")
(def library-html-resource (html/html-resource library-html {:parser jsoup/parser}))

;; BUTTON

(def anchor-button (html/select library-html-resource [:.clj-anchor-button]))

;; COOKIE MESSAGE

(def cookie-message-script (html/select library-html-resource [:.clj-cookie-message]))
(def cookie-message-library (html/select library-html-resource [:.clj-cookie-library]))

(defn add-cookie-message [context nodes]
  (let [translations (:translations context)]
    (if (:cookie-message-enabled config/environment)
      (html/at nodes
               [:head] (html/append cookie-message-library cookie-message-script)
               [:.clj-cookie-message html/text-node] (html/replace-vars {:cookieLink      (str utils/host-url "/cookies")
                                                                         :cookieMessage   (translations :cookie-banner/message)
                                                                         :cookieDismiss   (translations :cookie-banner/dismiss)
                                                                         :cookieLearnMore (translations :cookie-banner/learn-more)}))
      nodes)))

;; GOOGLE ANALYTICS

(def google-analytics-script (html/select library-html-resource [:.clj-google-analytics]))

(defn add-google-analytics [nodes]
  (if-let [tracking-id (:google-analytics-tracking-id config/environment)]
    (html/at nodes
             [:head] (html/append google-analytics-script)
             [:.clj-google-analytics html/text-node] (html/replace-vars {:trackingID tracking-id}))
    nodes))

(defn add-custom-app-name [context nodes]
  (if-let [app-name (:app-name config/environment)]
    (html/at nodes
             [:.masthead-logo] (html/content app-name)
             [:title] (html/content (get-in context [:doc :title])))
    nodes))

(defn remove-footer-alpha-warning [nodes]
  (if (not (:show-alpha-warnings config/environment))
    (html/at nodes
             [:.footer-alpha-warning] (html/substitute (html/html-snippet "")))
    nodes))

;; MASTHEAD

(def masthead-signed-out-snippet (html/select library-html-resource [:.clj-masthead-signed-out]))
(def masthead-signed-in-snippet (html/select library-html-resource [:.clj-masthead-signed-in]))

(defn masthead-signed-out [{:keys [ring-request] :as context}]
  (let [uri (:uri ring-request)]
    (html/at masthead-signed-out-snippet
             [:.clj-masthead-sign-in] (html/set-attr :href (str "/sign-in?refer=" uri)))))

(defn masthead-signed-in [{:keys [user] :as context}]
  (html/at masthead-signed-in-snippet
           [:.clj-masthead-admin-badge] (when (permissions/admin? user) identity)
           [:.clj-edit-profile] (if (permissions/writer? user)
                                  (html/set-attr :href (utils/path-for :fe/profile :username (:username user)))
                                  html/unwrap)
           [:.clj-pencil] (when (permissions/writer? user) identity)
           [:.clj-username] (html/content (:username user))))

(defn masthead [{:keys [user] :as context}]
  (if (:username user)
    (masthead-signed-in context)
    (masthead-signed-out context)))

;; STATUS BAR

(def flash-bar-snippet (first (html/select library-html-resource [:.clj-flash-message-bar])))
(def invitation-response-banner-snippet (html/select library-html-resource [:.clj-invitation-response-link]))
(def status-bar-snippet (html/select library-html-resource [:.clj-status-bar]))

(defn invitation-response-banner [{:keys [invitation-rsvp] :as context}]
  (html/at invitation-response-banner-snippet
           [:.clj-invitation-response-link]
           (html/set-attr :href (utils/local-path-for :fe/objective
                                                      :id (:objective-id invitation-rsvp)))))

(defn flash-bar [flash-message]
  (html/at flash-bar-snippet
           [:.clj-flash-message-bar-text] (html/content flash-message)))

(defn status-flash-bar [{:keys [doc invitation-rsvp translations] :as context}]
  (let [{type :type message :message} (:flash doc)]
    (cond
      (= type :flash-message) (flash-bar (translations message))
      invitation-rsvp (flash-bar (invitation-response-banner context))
      (not (:show-alpha-warnings config/environment)) (html/html-snippet "")
      :else status-bar-snippet)))

;; PROGRESS INDICATOR

(def progress-snippet (html/select library-html-resource [:.clj-objective-progress-indicator]))

(defn progress-indicator [{:keys [data] :as context}]
  (let [objective (:objective data)]
    (html/at progress-snippet
             [:.clj-progress-objective-link] (html/set-attr :href
                                                            (url/url (utils/path-for :fe/objective :id (:_id objective))))
             [:.clj-progress-drafts-link] (html/set-attr :href
                                                         (url/url (utils/path-for :fe/draft-list :id (:_id objective))))
             [:.clj-progress-draft-count] (html/content (str "(" (get-in objective [:meta :drafts-count]) ")")))))

;; WRITER LIST

(def empty-writer-list-item-snippet (html/select library-html-resource [:.clj-empty-writer-list-item]))

(def writer-item-snippet (html/select library-html-resource [:.clj-writer-item-without-photo]))

(defn writer-list-items [writers]
  (html/at writer-item-snippet
           [:.clj-writer-item-without-photo]
           (html/clone-for [writer writers]
                           [:.clj-writer-profile-link] (html/set-attr :href
                                                                      (utils/path-for :fe/profile
                                                                                      :username (:username writer)))
                           [:.clj-writer-name] (html/content (if-let [real-name (:name (:profile writer))]
                                                               real-name
                                                               (:username writer)))
                           [:.clj-writer-description] (html/content (:invitation-reason writer)))))

(defn writer-list [context]
  (let [writers (get-in context [:data :writers])]
    (if (empty? writers)
      empty-writer-list-item-snippet
      (writer-list-items writers))))

;; COMMENT LIST

(defn comment-refer-uri [request comment]
  (str (utils/referer-url request)
       "#comment-" (:_id comment)))

(defn voting-actions-when-signed-in [{:keys [data ring-request anti-forgery-snippet] :as context} comment]
  (html/transformation
    :lockstep
    {[:.clj-up-down-vote-form] (html/prepend anti-forgery-snippet)
     [:.clj-vote-on-uri]       (html/set-attr "value" (:uri comment))
     [:.clj-refer]             (html/set-attr "value" (comment-refer-uri ring-request comment))
     [:.clj-up-vote-count]     (html/content (str (get-in comment [:votes :up])))
     [:.clj-down-vote-count]   (html/content (str (get-in comment [:votes :down])))}))

(defn voting-actions-when-not-signed-in [{:keys [data ring-request] :as context} comment]
  (html/transformation
    :lockstep
    {[:.clj-up-down-vote-form] (html/do-> (html/set-attr :method "get") (html/set-attr :action "/sign-in"))
     [:.clj-refer]             (html/set-attr "value" (comment-refer-uri ring-request comment))
     [:.clj-vote-on-uri]       nil
     [:.clj-up-vote-count]     (html/content (str (get-in comment [:votes :up])))
     [:.clj-down-vote-count]   (html/content (str (get-in comment [:votes :down])))}))

(def empty-comment-list-item-snippet (html/select library-html-resource [:.clj-empty-comment-list-item]))

(def comment-list-item-snippet (html/select library-html-resource [:.clj-comment-item]))

(def comment-list-item-removal-container (html/select comment-list-item-snippet [:.clj-comment-item-removal-container]))

(defn removal-container [anti-forgery-snippet {:keys [comment-on-uri comment uri] :as the-comment}]
  (html/at comment-list-item-removal-container
             [:.clj-comment-removal-form] (html/prepend anti-forgery-snippet)
             [:.clj-removal-uri] (html/set-attr :value uri)
             [:.clj-removal-sample] (html/set-attr :value comment)
             [:.clj-comment-on-uri] (html/set-attr :value comment-on-uri)
           ))

(defn comment-item [{:keys [anti-forgery-snippet user translations] :as context} comment]
  (html/at comment-list-item-snippet
           :lockstep
           {[:.clj-comment-item]                   (html/set-attr :id (str "comment-" (:_id comment)))
            [:.clj-comment-author]                 (html/content (:username comment))
            [:.clj-comment-date]                   (html/content (utils/iso-time-string->pretty-time (:_created_at comment)))
            [:.clj-comment-text]                   (html/content (:comment comment))
            [:.clj-comment-reason-text]            (when (:reason comment)
                                                     (html/content
                                                       (translations (keyword "add-comment-form" (str "comment-reason-" (:reason comment))))))
            [:.clj-up-down-vote-form]
                                                   (if (:username user)
                                                     (voting-actions-when-signed-in context comment)
                                                     (voting-actions-when-not-signed-in context comment))
            [:.clj-writer-note-item-container]     (when (:note comment) identity)
            [:.clj-writer-note-item-content]       (html/content (:note comment))
            [:.clj-comment-reply]                  nil
            [:.clj-comment-item-removal-container] (when (permissions/admin? user)
                                                     (html/substitute (removal-container anti-forgery-snippet comment)))}))

(defn comment-list-items [{:keys [data user translations] :as context} comments]
  (html/at comment-list-item-snippet
           [:.clj-comment-item]
           (html/clone-for [comment comments]
                           [:.clj-comment-item] (html/substitute (comment-item context comment)))))

(defn comment-list [{:keys [data] :as context}]
  (let [comments (get-in data [:comments-data :comments])]
    (if (empty? comments)
      empty-comment-list-item-snippet
      (comment-list-items context comments))))

;; COMMENT CREATE

(def comment-create-form-snippet (html/select library-html-resource [:.clj-add-comment-form]))

(defn apply-comment-form-validations [{:keys [doc] :as context} nodes]
  (let [validation-data (get-in doc [:flash :validation])
        validation-report (:report validation-data)
        previous-inputs (:data validation-data)]
    (html/at nodes
             [:.clj-comment-length-error] (when (contains? (:comment validation-report) :length) identity)
             [:.clj-comment-empty-error] (when (contains? (:comment validation-report) :empty) identity)
             [:.clj-input-comment] (html/content (:comment previous-inputs)))))

(defn comment-create-form [{:keys [anti-forgery-snippet data doc ring-request translations] :as context} comment-target]
  (let [page-name (:page-name doc)]
    (->> (html/at comment-create-form-snippet
                  [:.clj-add-comment-form] (html/prepend anti-forgery-snippet)
                  [:.clj-add-comment-form] (if (= :section comment-target)
                                             (html/set-attr :action (str (get-in data [comment-target :uri]) "/annotations"))
                                             identity)
                  [:.clj-reason] (when (= :section comment-target) identity)
                  [:.clj-refer] (html/set-attr :value (:uri ring-request))
                  [:.clj-comment-on-uri] (html/set-attr :value (get-in data [comment-target :uri]))
                  [:.clj-comment-form-label-title] (html/content (translations (keyword page-name "comment-box-label-title")))
                  [:.clj-comment-form-label-helper] (html/content (translations (keyword page-name "comment-box-label-helper")))
                  [:.clj-comment-form-post-button] (html/content (translations (keyword page-name "comment-post-button"))))
         (apply-comment-form-validations context))))

(def sign-in-to-comment-snippet (html/select library-html-resource [:.clj-to-comment-please-sign-in]))

(defn sign-in-to-comment [{:keys [ring-request] :as context}]
  (html/at sign-in-to-comment-snippet
           [:.clj-to-comment-sign-in-link]
           (html/set-attr :href (str "/sign-in?refer=" (utils/referer-url ring-request) "%23add-comment-form"))))


(defn comment-create [{user :user :as context} comment-target]
  (if (:username user)
    (comment-create-form context comment-target)
    (sign-in-to-comment context)))

;; ANSWERS

(def answer-with-no-saved-item-snippet (html/select library-html-resource [:.cjl-dashboard-answer-with-no-saved-item]))

(def answer-with-saved-item-snippet (html/select library-html-resource [:.cjl-dashboard-answer-with-saved-item]))
