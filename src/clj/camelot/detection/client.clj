(ns camelot.detection.client
  "Client for the detection API."
  (:require
   [camelot.util.version :as version]
   [clojure.tools.logging :as log]
   [clj-http.client :as http]
   [cheshire.core :as json]))

(def socket-timeout (* 30 1000))
(def connection-timeout (* 5 1000))

;; TODO handle failure / retry
(defn- http-get
  [state endpoint]
  (let [resp (http/get (str (-> state :config :detector :api-url) endpoint)
                       {:accept "json"
                        :socket-timeout socket-timeout
                        :connection-timeout connection-timeout
                        :basic-auth [(-> state :config :detector :username)
                                     (-> state :config :detector :password)]
                        :headers {"x-camelot-version" (version/get-version)}})]
    (json/parse-string (:body resp) true)))

(defn- http-post
  [state endpoint]
  (let [resp (http/post (str (-> state :config :detector :api-url) endpoint)
                        {:accept "json"
                         :socket-timeout socket-timeout
                         :connection-timeout connection-timeout
                         :basic-auth [(-> state :config :detector :username)
                                      (-> state :config :detector :password)]
                         :headers {"x-camelot-version" (version/get-version)}})]
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
  ;; TODO implement me
  nil)
