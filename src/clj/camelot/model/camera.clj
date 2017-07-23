(ns camelot.model.camera
  "Camera model and data-access."
  (:require
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.system.state :refer [State]]
   [yesql.core :as sql]
   [camelot.model.camera-status :as camera-status]
   [camelot.model.media :as media]))

(sql/defqueries "sql/cameras.sql")

(s/defrecord TCamera
    [camera-name :- s/Str
     camera-make :- (s/maybe s/Str)
     camera-model :- (s/maybe s/Str)
     camera-notes :- (s/maybe s/Str)
     camera-status-id :- s/Num]
  {s/Any s/Any})

(s/defrecord Camera
    [camera-id :- s/Num
     camera-created :- org.joda.time.DateTime
     camera-updated :- org.joda.time.DateTime
     camera-name :- s/Str
     camera-make :- (s/maybe s/Str)
     camera-model :- (s/maybe s/Str)
     camera-notes :- (s/maybe s/Str)
     camera-status-id :- s/Num
     camera-status-description :- s/Str]
  {s/Any s/Any})

(def camera map->Camera)
(defn tcamera
  [ks]
  (map->TCamera (update ks :camera-status-id #(or % 1))))

(s/defn to-camera :- Camera
  [state record]
  (-> record
      (update :camera-status-description
              #(camera-status/translate-status state %))
      camera))

(s/defn get-all :- [Camera]
  [state :- State]
  (map #(to-camera state %)
       (db/clj-keys (db/with-connection state -get-all))))

(s/defn get-available :- [Camera]
  [state :- State]
  (->> (db/with-connection state -get-available)
       db/clj-keys
       (map #(to-camera state %))))

(s/defn get-specific :- (s/maybe Camera)
  [state :- State
   id :- s/Num]
  (some->> {:camera-id id}
           (db/with-db-keys state -get-specific)
           (first)
           (to-camera state)))

(s/defn get-specific-by-name :- (s/maybe Camera)
  [state :- State
   data :- {:camera-name s/Str}]
  (some->> data
           (db/with-db-keys state -get-specific-by-name)
           first
           (to-camera state)))

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

(defn make-available
  "Set the associated cameras' status to 'available'.
  `cameras' is a coll of camera IDs."
  [state cameras]
  (let [available-status (camera-status/available-status-id state)]
    (doseq [cam-id (distinct cameras)]
      (set-camera-status! state cam-id available-status))))
