(ns d-cent.objectives-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [d-cent.objectives :refer :all]))

(defn requestify [params]
  {:params params})

(def test-objective {:title "My Objective"
                    :goals ["To rock out" "All day"]
                    :description "I like cake"
                    :end-date "2015-01-31"})

(fact "gets an objective from a request"
      (request->objective (requestify test-objective)) => (assoc test-objective :username "username")
      (provided
        (friend/current-authentication) => {:username "username"}))

(fact "returns nil if extra params are in the request"
      (request->objective (requestify (assoc test-objective :extra-stuff "Blaaaaaaaaah"))) => nil)
