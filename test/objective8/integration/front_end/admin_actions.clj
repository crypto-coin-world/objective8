(ns objective8.integration.front-end.admin-actions
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [oauth.client :as oauth]
            [objective8.front-end.api.http :as http-api]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as ih]))

(def USER_ID 1)
(def USER_URI (str "/users/" USER_ID))
(def OBJECTIVE_ID 234)
(def OBJECTIVE_URI (str "/objectives/" OBJECTIVE_ID))
(def COMMENT_ID 456)
(def COMMENT_URI (str "/comments/" COMMENT_ID))
(def TWITTER_ID "twitter-123456")

(facts "about confirming admin-removals"
       (binding [config/enable-csrf false]
         (fact "admin can post an admin-removal confirmation for an objective"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}})
               (against-background
                 (http-api/post-admin-removal {:removal-uri    OBJECTIVE_URI
                                               :removed-by-uri USER_URI}) => {:status ::http-api/success})
               (let [user-session (ih/front-end-context)
                     params {:removal-uri OBJECTIVE_URI}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/admin-removal-confirmation-post)
                                                         :request-method :post
                                                         :params params))]
                 (:headers response) => (ih/location-contains (utils/path-for :fe/objective-list))
                 (:status response) => 302))

         (fact "admin cannot reach the admin-removal confirmation page without valid removal-data parameters in the session"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}})
               (let [user-session (ih/front-end-context)
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/admin-removal-confirmation-get)))]
                 (:status response) => 404))))

(facts "about admin-removals"
       (binding [config/enable-csrf false]
         (fact "admin can post an admin-removal for an objective"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}})
               (let [user-session (ih/front-end-context)
                     params {:removal-uri    OBJECTIVE_URI
                             :removal-sample "Objective Title"}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-admin-removal)
                                                         :request-method :post
                                                         :params params)
                                              p/follow-redirect)]
                 (:status response) => 200
                 (:body response) => (contains "Objective Title")
                 (:body response) => (contains (:removal-uri params))))))

(facts "about removing comments"
       (binding [config/enable-csrf false]
         (fact "submit a request to delete a comment redirects the admin to the confirmation page"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}})

               (let [user-session (ih/front-end-context)
                     params {:removal-uri    COMMENT_URI
                             :removal-sample "The comment"
                             :comment-on-uri OBJECTIVE_URI}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-comment-removal)
                                                         :request-method :post
                                                         :params params)
                                              p/follow-redirect)]
                 (:status response) => 200
                 (:body response) => (contains "The comment")
                 (:body response) => (contains (:removal-uri params))
                 (:body response) => (contains (:comment-on-uri params))))

         (fact "Remove comment confirmation page posts to the correct endpoints"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}})

               (let [user-session (ih/front-end-context)
                     params {:removal-uri    COMMENT_URI
                             :removal-sample "The comment"
                             :comment-on-uri OBJECTIVE_URI}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-comment-removal)
                                                         :request-method :post
                                                         :params params)
                                              p/follow-redirect)]
                 (:status response) => 200
                 (:body response) =not=> (contains "/meta/admin-removal-confirmation")
                 (:body response) => (contains "/meta/remove-comment-confirmation")

                 (:body response) => (contains (str "href=\"" OBJECTIVE_URI))
                 ))


         (fact "admin can submit a removal confirmation for a comment"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}})

               (against-background
                 (http-api/post-comment-removal {:removal-uri    COMMENT_URI
                                                 :removed-by-uri USER_URI
                                                 :comment-on-uri OBJECTIVE_URI}) => {:status ::http-api/success})

               (let [user-session (ih/front-end-context)
                     params {:removal-uri    COMMENT_URI
                             :comment-on-uri OBJECTIVE_URI}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-comment-removal-confirmation)
                                                         :request-method :post
                                                         :params params))]
                 (:headers response) => (ih/location-contains OBJECTIVE_URI)
                 (:status response) => 302))

         (fact "invalid comment removal request returns 400"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}})

               (against-background
                 (http-api/post-comment-removal {:removal-uri    COMMENT_URI
                                                 :removed-by-uri USER_URI
                                                 :comment-on-uri OBJECTIVE_URI}) => {:status ::http-api/invalid-input})

               (let [user-session (ih/front-end-context)
                     params {:removal-uri    COMMENT_URI
                             :comment-on-uri OBJECTIVE_URI}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-comment-removal-confirmation)
                                                         :request-method :post
                                                         :params params))]
                 (:status response) => 400))

         (fact "returns 502 if any other error occurs with the backend API"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}})

               (against-background
                 (http-api/post-comment-removal {:removal-uri    COMMENT_URI
                                                 :removed-by-uri USER_URI
                                                 :comment-on-uri OBJECTIVE_URI}) => {:status ::http-api/somethingelse})

               (let [user-session (ih/front-end-context)
                     params {:removal-uri    COMMENT_URI
                             :comment-on-uri OBJECTIVE_URI}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-comment-removal-confirmation)
                                                         :request-method :post
                                                         :params params))]
                 (:status response) => 502))))


(facts "about promoting objectives"
       (binding [config/enable-csrf false]
         (fact "admin can post a promote objective form for an objective"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/post-promote-objective {:objective-uri "1" :promoted-by "/users/1"}) => {:status ::http-api/success}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}}
                 (http-api/get-objectives {:signed-in-id 1}) => {:status ::http-api/success
                                                                 :result {:some :data}})
               (let [user-session (ih/front-end-context)
                     params {:objective-uri 1}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-promote-objective)
                                                         :request-method :post
                                                         :params params))]
                 (:status response) => 302
                 (:headers response) => (ih/location-contains (utils/path-for :fe/objective-list))))

         (fact "returns 400 if invalid input is entered"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/post-promote-objective {:objective-uri "1" :promoted-by "/users/1"}) => {:status ::http-api/invalid-input
                                                                                                    :result [{}]}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}}
                 (http-api/get-objectives {:signed-in-id 1}) => {:status ::http-api/success
                                                                 :result {:some :data}})

               (let [user-session (ih/front-end-context)
                     params {:objective-uri 1}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-promote-objective)
                                                         :request-method :post
                                                         :params params))]
                 (:status response) => 400))
         (fact "returns 502 if any other error occurs with the back end API"
               (against-background
                 (oauth/access-token anything anything anything) => {:user_id TWITTER_ID}
                 (http-api/post-promote-objective {:objective-uri "1" :promoted-by "/users/1"}) => {:status ::http-api/aesrdtfyguh
                                                                                                    :result [{:rs "drtfyghj"}]}
                 (http-api/find-user-by-auth-provider-user-id anything) => {:status ::http-api/success
                                                                            :result {:_id      USER_ID
                                                                                     :username "username"}}
                 (http-api/get-user anything) => {:result {:admin true}}
                 (http-api/get-objectives {:signed-in-id 1}) => {:status ::http-api/success
                                                                 :result {:some :data}})

               (let [user-session (ih/front-end-context)
                     params {:objective-uri 1}
                     {response :response} (-> user-session
                                              ih/sign-in-as-existing-user
                                              (p/request (utils/path-for :fe/post-promote-objective)
                                                         :request-method :post
                                                         :params params))]
                 (:status response) => 502))))

(facts "about admin activity"
       (fact "admin removals can be retrieved"
             (against-background
               (http-api/get-admin-removals) => {:status ::http-api/success
                                                 :result [{:removal-uri OBJECTIVE_URI
                                                           :_created_at "2015-03-24T17:06:37.714Z"}]})
             (let [{response :response} (-> (ih/front-end-context)
                                            (p/request (utils/path-for :fe/admin-activity)))]
               (:status response) => 200
               (:body response) => (contains OBJECTIVE_URI))))
