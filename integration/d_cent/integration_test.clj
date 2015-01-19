(ns d-cent.integration-test
  (:require [ring.mock.request :as mock]
            [midje.sweet :refer :all]
            [d-cent.core :as core]))

(def create-proposal-request (mock/request :get "/create-proposal"))

(defn with-signed-in-user [request]
  (into request {:session 
                 {:cemerick.friend/identity 
                  {:authentications 
                   {"screen name" {:identity "screen name" 
                                   :username "screen name"
                                   :roles #{:signed-in}}}
                   :current "screen name"}}}))

(facts "About creating a proposal"
       (fact "A signed in user receives an html response"
             (core/app (-> create-proposal-request with-signed-in-user))
             => (contains {:status 200}))
       (fact "A user who isn't signed in is redirected to login"
             (core/app create-proposal-request)
             => (contains {:status 302})))

