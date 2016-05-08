(ns camelot.handler.camera-statuses
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.util.java-file :as jf]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.camera :refer [CameraStatus]]))

(sql/defqueries "sql/camera-status.sql" {:connection db/spec})

(s/defn get-all :- [CameraStatus]
  [state]
  (map #(assoc % :camera-status-description
               ((:translate state) (keyword (:camera-status-description %))))
       (db/clj-keys (-get-all))))

(def routes
  (context "/camera-statuses" []
           (GET "/available/" [] (rest/list-resources get-all :camera-status))
           (GET "/alternatives/:id" [id] (rest/list-resources get-all :camera-status))))
