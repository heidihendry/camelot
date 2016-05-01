(ns camelot.handler.camera-status
  (:require [camelot.util.java-file :as jf]
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
