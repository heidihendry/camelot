(ns camelot.handler.cameras
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.camera :refer [Camera CameraCreate]]))

(sql/defqueries "sql/cameras.sql" {:connection db/spec})

(s/defn get-all :- [Camera]
  [state]
  (db/clj-keys (-get-all)))

(s/defn get-specific :- Camera
  [state
   id :- s/Num]
  (first (db/with-db-keys -get-specific {:camera-id id})))

(s/defn get-specific-by-name :- Camera
  [state
   data :- {:camera-name s/Str}]
  (db/with-db-keys -get-specific-by-name data))

(s/defn create!
  [state
   data :- CameraCreate]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   data :- Camera]
  (db/with-db-keys -update! data)
  (get-specific state (:camera-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:camera-id id}))
