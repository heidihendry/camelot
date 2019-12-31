(ns camelot.http.detector
  (:require
   [clojure.data.json :as json]
   [compojure.core :refer [context GET]]))

(defn- detector-status
  [state]
  (if-let [events-atom (-> state :detector :events)]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str @events-atom)}
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body {:status :offline}}))

(def routes
  (context "/detector" {state :system}
           (GET "/status" [] (detector-status state))))
