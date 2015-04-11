(ns objective8.middleware
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.middleware.ssl :refer  [wrap-ssl-redirect wrap-hsts]]
            [objective8.config :as config]
            [objective8.utils :as utils]))

(defn- keywordize [m]
  (into {} (for [[k v] m] [(keyword k) v])))

(defn valid-credentials? [tokens-fn bearer-name provided-token]
  (when bearer-name
    (when-let [stored-token (tokens-fn bearer-name)]
      (= provided-token stored-token))))

(defn wrap-bearer-token
  "Middleware for authorising api requests.
  Tokens-fn should be a function that takes
  bearer name and returns a token or nil."
  [handler tokens-fn]
  (fn [{:keys [headers] :as request}]
    (let [bearer-token (get headers "api-bearer-token")
          bearer-name (get headers "api-bearer-name")]
      (if (valid-credentials? tokens-fn bearer-name bearer-token)
        (handler (update-in request [:headers] dissoc "api-bearer-token" "api-bearer-name"))
        (do (log/info (str "Invalid bearer token credentials"))
            {:status 401})))))

(defn wrap-not-found [handler not-found-handler]
  (fn [request]
    (if-let [response (handler request)]
      response
      (not-found-handler request))))

(defn strip-trailing-slashes [handler]
  (fn [request]
    (handler (update-in request [:uri] s/replace #"(.)/$" "$1"))))

(defn wrap-ssl-redirect-and-hsts [handler]
  (fn [request]
    (if (config/get-var "ENABLE_HTTPS_ONLY")
      (wrap-ssl-redirect request) 
      request)))

(defn wrap-authorise-writer [handler]
  (fn [{{objective-id :id} :route-params :as request}]
    (let [roles #{(utils/writer-for objective-id)}]
      (if (friend/authorized? roles (friend/identity request))
        (handler request)
        (friend/throw-unauthorized (friend/identity request)
                                   {::friend/wrapped-handler handler
                                    ::friend/required-roles roles})))))

