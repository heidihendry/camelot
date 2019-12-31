(ns camelot.detection.client
  "Client for the detection API."
  (:require
   [clojure.tools.logging :as log]
   [clj-http.client :as http]
   [cheshire.core :as json]))

(def socket-timeout (* 30 1000))
(def connection-timeout (* 5 1000))

;; TODO handle failure / retry
;;
;; TODO include UA (camelot version) in header for handling misbehaviour.
;; Better to disable old clients than be stuck with misbehaving ones.
(defn- http-get
  [state endpoint]
  (let [resp (http/get (str (-> state :config :detector :api-url) endpoint)
                       {:accept "json"
                        :socket-timeout socket-timeout
                        :connection-timeout connection-timeout
                        :basic-auth [(-> state :config :detector :username)
                                     (-> state :config :detector :password)]})]
    (json/parse-string (:body resp) true)))

(defn- http-post
  [state endpoint]
  (println "POST")
  (log/info "Starting request")
  (println (str (-> state :config :detector :api-url) endpoint))
  (try
    (let [resp (http/post (str (-> state :config :detector :api-url) endpoint)
                          {:accept "json"
                           :socket-timeout socket-timeout
                           :connection-timeout connection-timeout
                           :basic-auth [(-> state :config :detector :username)
                                        (-> state :config :detector :password)]})]
      (println "got it?")
      (log/error "Request succeeded")
      (json/parse-string (:body resp) true))
    (catch Exception e
      (println "Failed")
      (println e))))

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
