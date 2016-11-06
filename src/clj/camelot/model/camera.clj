(ns camelot.model.camera
  (:require [schema.core :as s]
            [camelot.db :as db]
            [camelot.model.state :refer [State]]
            [yesql.core :as sql]
            [camelot.model.camera-status :as camera-status]
            [camelot.application :as app]
            [camelot.util.config :as config]
            [camelot.model.media :as media]))

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
     camera-status-id :- s/Num
     camera-status-description :- s/Str])

(s/defn camera :- Camera
  [{:keys [camera-id camera-created camera-updated camera-name
           camera-make camera-model camera-notes camera-status-id
           camera-status-description]}]
  (->Camera camera-id camera-created camera-updated camera-name
            camera-make camera-model camera-notes camera-status-id
            (camera-status/translate-status camera-status-description)))

(s/defn tcamera :- TCamera
  [{:keys [camera-name camera-make camera-model camera-notes
           camera-status-id]}]
  (->TCamera camera-name camera-make camera-model camera-notes
             (or camera-status-id (:camera-status-id
                                   (camera-status/default-camera-status)))))

(s/defn get-all :- [Camera]
  [state :- State]
  (map camera (db/clj-keys (db/with-connection (:connection state) -get-all))))

(s/defn get-available :- [Camera]
  [state :- State]
  (map camera (db/clj-keys (db/with-connection (:connection state) -get-available))))

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
   id :- s/Int]
  (let [fs (media/get-all-files-by-camera state id)]
    (db/with-db-keys state -delete! {:camera-id id})
    (media/delete-files! state fs))
  nil)

(s/defn get-or-create! :- Camera
  [state :- State
   data :- TCamera]
  (or (get-specific-by-name state (select-keys data [:camera-name]))
      (create! state data)))

(s/defn set-camera-status!
  [state :- State
   cam-id :- s/Int
   cam-status :- s/Int]
  (db/with-db-keys state -set-camera-status!
    {:camera-id cam-id
     :camera-status-id cam-status}))
