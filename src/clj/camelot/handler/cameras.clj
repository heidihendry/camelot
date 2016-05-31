(ns camelot.handler.cameras
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.camera :refer [Camera CameraCreate]]))

(sql/defqueries "sql/cameras.sql" {:connection db/spec})

(s/defn get-all :- [Camera]
  [state]
  (db/clj-keys (db/with-connection (:connection state) -get-all)))

(s/defn get-specific :- Camera
  [state
   id :- s/Num]
  (first (db/with-db-keys state -get-specific {:camera-id id})))

(s/defn get-specific-by-name :- (s/maybe Camera)
  [state
   data :- {:camera-name s/Str}]
  (first (db/with-db-keys state -get-specific-by-name data)))

(s/defn create!
  [state
   data :- CameraCreate]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   id :- s/Num
   data :- Camera]
  (db/with-db-keys state -update! (merge data {:camera-id id}))
  (get-specific state (:camera-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys state -delete! {:camera-id id}))

(def routes
  (context "/cameras" []
           (GET "/" [] (rest/list-resources get-all :camera))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
