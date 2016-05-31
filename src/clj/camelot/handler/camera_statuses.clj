(ns camelot.handler.camera-statuses
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.util.java-file :as jf]
            [camelot.translation.core :as tr]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.camera :refer [CameraStatus]]))

(sql/defqueries "sql/camera-status.sql" {:connection db/spec})

(defn- translate-statuses
  "Translate the description of camera statuses."
  [state statuses]
  (map #(assoc % :camera-status-description
               (tr/translate (:config state) (keyword (:camera-status-description %))))
       statuses))

(s/defn get-all :- [CameraStatus]
  "Retrieve, translate and return all available camera statuses."
  [state]
  (->> (db/with-connection (:connection state) -get-all)
       (db/clj-keys)
       (translate-statuses state)))

(def routes
  (context "/camera-statuses" []
           (GET "/available/" [] (rest/list-resources get-all :camera-status))
           (GET "/alternatives/:id" [id] (rest/list-resources get-all :camera-status))))
