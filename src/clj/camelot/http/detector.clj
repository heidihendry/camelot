(ns camelot.http.detector
  (:require
   [clojure.core.async :as async]
   [clojure.data.json :as json]
   [ring.util.http-response :as hr]
   [compojure.core :refer [context GET POST]]
   [clojure.tools.logging :as log]))

(defn- detector-status
  [state]
  (if-let [detector-state (-> state :detector :state deref)]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str (select-keys detector-state [:system-status :events]))}
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body {:system-status :stopped}}))

(def routes
  (context "/detector" {state :system}
           (GET "/status" [] (detector-status state))
           (POST "/command" [data]
                 (do
                   (log/error "Error!" data)
                   (let [{:keys [cmd]} data]
                     (if (and (#{:pause :resume :rerun} cmd) (-> state :detector :cmd-chan))
                       (do
                         (async/put! (-> state :detector :cmd-chan) {:cmd cmd})
                         (hr/no-content))
                       (hr/bad-request)))))))
