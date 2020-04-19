(ns camelot.http.sighting-field-value
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET]]
   [camelot.model.sighting-field-value :as sighting-field-value]))

(def routes
  (context "/sighting-field-values" {state :state}
           (GET "/" [] (r/response (vals (sighting-field-value/query-all state))))))
