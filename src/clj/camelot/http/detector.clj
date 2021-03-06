(ns camelot.http.detector
  (:require
   [clojure.data.json :as json]
   [ring.util.http-response :as hr]
   [compojure.core :refer [context GET POST]]
   [camelot.state.datasets :as datasets]))

(defn get-detector-for-dataset
  [state]
  (let [dataset-id (datasets/get-dataset-context (:datasets state))]
    @(-> state :detector :state :datasets dataset-id)))

(defn- detector-status
  [state]
  (if-let [detector-state (get-detector-for-dataset state)]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str {:events (:events detector-state) :system @(-> state :detector :state :system)})}
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body {:system {:status :stopped}}}))

(def routes
  (context "/detector" {state :state}
           (GET "/status" [] (detector-status state))
           (POST "/command" [data]
                 (let [{:keys [cmd]} data]
                   (if (and (#{:pause :resume :rerun} cmd))
                     (do
                       (.command (:detector state) {:cmd cmd :set-by :user})
                       (hr/no-content))
                     (hr/bad-request))))))
