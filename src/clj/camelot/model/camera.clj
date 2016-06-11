(ns camelot.model.camera
  (:require [schema.core :as s]
            [camelot.db :as db]
            [camelot.model.state :refer [State]]
            [yesql.core :as sql]))

(sql/defqueries "sql/cameras.sql" {:connection db/spec})

(s/defrecord TCamera
    [camera-name :- s/Str
     camera-make :- (s/maybe s/Str)
     camera-model :- (s/maybe s/Str)
     camera-notes :- (s/maybe s/Str)
     camera-status-id :- s/Num])

(s/defrecord Camera
    [camera-id :- s/Num
     camera-created :- org.joda.time.DateTime
     camera-updated :- org.joda.time.DateTime
     camera-name :- s/Str
     camera-make :- (s/maybe s/Str)
     camera-model :- (s/maybe s/Str)
     camera-notes :- (s/maybe s/Str)
     camera-status-id :- s/Num])

(s/defn camera :- Camera
  [{:keys [camera-id camera-created camera-updated camera-name
           camera-make camera-model camera-notes camera-status-id]}]
  (->Camera camera-id camera-created camera-updated camera-name
            camera-make camera-model camera-notes camera-status-id))

(s/defn tcamera :- TCamera
  [{:keys [camera-name camera-make camera-model camera-notes
           camera-status-id]}]
  (->TCamera camera-name camera-make camera-model camera-notes
                  camera-status-id))

(s/defn get-all :- [Camera]
  [state :- State]
  (map camera (db/clj-keys (db/with-connection (:connection state) -get-all))))

(s/defn get-specific :- (s/maybe Camera)
  [state :- State
   id :- s/Num]
  (some->> {:camera-id id}
           (db/with-db-keys state -get-specific)
           (first)
           (camera)))

(s/defn get-specific-by-name :- (s/maybe Camera)
  [state :- State
   data :- {:camera-name s/Str}]
  (some->> data
           (db/with-db-keys state -get-specific-by-name)
           (first)
           (camera)))

(s/defn create!
  [state :- State
   data :- TCamera]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (int (:1 record)))))

(s/defn update!
  [state :- State
   id :- s/Num
   data :- TCamera]
  (db/with-db-keys state -update! (merge data {:camera-id id}))
  (get-specific state id))

(s/defn delete!
  [state :- State
   id :- s/Num]
  (db/with-db-keys state -delete! {:camera-id id}))
