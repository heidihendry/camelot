(ns camelot.http.sighting-field-value
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET]]
   [camelot.model.sighting-field-value :as sighting-field-value]))

(def routes
  (context "/sighting-field-values" {session :session state :system}
           (GET "/" [] (r/response (vals (sighting-field-value/query-all (assoc state :session session)))))))
