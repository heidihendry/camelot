(ns camelot.detection.client
  "Client for the detection API."
  (:require
   [camelot.util.version :as version]
   [clj-http.client :as http]
   [clj-http.conn-mgr :as conn-mgr]
   [cheshire.core :as json]
   [diehard.core :as dh])
  (:import [net.jodah.failsafe CircuitBreakerOpenException]))

(defonce ^:private connection-manager
  (conn-mgr/make-reusable-conn-manager {}))

(defn- request-config
  [state]
  {:accept "json"
   :connection-manager connection-manager
   :socket-timeout (* 30 1000)
   :connection-timeout (* 5 1000)
   :basic-auth [(-> state :config :detector :username)
                (-> state :config :detector :password)]
   :headers {"x-camelot-version" (version/get-version)}})

(def ^:private retry-policy
  {:max-retries 5
   :backoff-ms [1000 10000 2.0]
   :jitter-factor 0.1})

(defn- http-get
  [state endpoint]
  (let [resp (dh/with-retry retry-policy
               (http/get (str (-> state :config :detector :api-url) endpoint)
                         (request-config state)))]
    (json/parse-string (:body resp) true)))

(defn- http-post
  [state endpoint]
  (let [resp (dh/with-retry retry-policy
               (http/post (str (-> state :config :detector :api-url) endpoint)
                          (request-config state)))]
    (json/parse-string (:body resp) true)))

(defn account-auth
  "Verify the user can authenticate with the given credentials."
  [state]
  (http-post state "/account/auth"))

(defn create-task
  "Create a task."
  [state]
  (http-post state "/task"))

(defn submit-task
  "Submit a task."
  [state task-id]
  (http-post state (format "/task/%s/submit" task-id)))

(defn get-task
  "Get a task."
  [state task-id]
  (http-get state (format "/task/%s" task-id)))

(defn archive-task
  "Archive a task."
  [state task-id]
  (http-post state (format "/task/%s/archive" task-id)))
