(ns camelot.http.camera-status
  (:require
   [compojure.core :refer [context GET]]
   [camelot.model.camera-status :as camera-status]
   [camelot.util.crud :as crud]))

(def routes
  (context "/camera-statuses" {session :session state :system}
           (GET "/available/" [] (crud/list-resources camera-status/get-all :camera-status (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-resources camera-status/get-all :camera-status (assoc state :session session)))))
