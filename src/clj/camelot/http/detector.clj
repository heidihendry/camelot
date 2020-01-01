(ns camelot.http.detector
  (:require
   [clojure.data.json :as json]
   [compojure.core :refer [context GET]]))

(defn- detector-status
  [state]
  (if-let [detector-state (-> state :detector :state deref)]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str (select-keys detector-state [:system-status :events]))}
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body {:system-status :offline}}))

(def routes
  (context "/detector" {state :system}
           (GET "/status" [] (detector-status state))))
