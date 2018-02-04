(ns camelot.model.camera
  "Camera model and data-access."
  (:require
   [schema.core :as sch]
   [clojure.spec.alpha :as s]
   [camelot.spec.system :as sysspec]
   [camelot.util.db :as db]
   [camelot.spec.schema.state :refer [State]]
   [camelot.model.camera-status :as camera-status]
   [camelot.model.media :as media]))

(def query (db/with-db-keys :cameras))

(sch/defrecord TCamera
    [camera-name :- sch/Str
     camera-make :- (sch/maybe sch/Str)
     camera-model :- (sch/maybe sch/Str)
     camera-notes :- (sch/maybe sch/Str)
     camera-status-id :- sch/Num]
  {sch/Any sch/Any})

(sch/defrecord Camera
    [camera-id :- sch/Num
     camera-created :- org.joda.time.DateTime
     camera-updated :- org.joda.time.DateTime
     camera-name :- sch/Str
     camera-make :- (sch/maybe sch/Str)
     camera-model :- (sch/maybe sch/Str)
     camera-notes :- (sch/maybe sch/Str)
     camera-status-id :- sch/Num
     camera-status-description :- sch/Str]
  {sch/Any sch/Any})

(def camera map->Camera)
(defn tcamera
  [ks]
  (map->TCamera (update ks :camera-status-id #(or % 1))))

(sch/defn to-camera :- Camera
  [state record]
  (-> record
      (update :camera-status-description
              #(camera-status/translate-status state %))
      camera))

(sch/defn get-all :- [Camera]
  [state :- State]
  (map #(to-camera state %)
       (query state :get-all)))

(sch/defn get-available :- [Camera]
  [state :- State]
  (->> (query state :get-available)
       (map #(to-camera state %))))

(sch/defn get-specific :- (sch/maybe Camera)
  [state :- State
   id :- sch/Num]
  (some->> {:camera-id id}
           (query state :get-specific)
           (first)
           (to-camera state)))

(sch/defn get-specific-by-name :- (sch/maybe Camera)
  [state :- State
   data :- {:camera-name sch/Str}]
  (some->> data
           (query state :get-specific-by-name)
           first
           (to-camera state)))

(sch/defn create!
  [state :- State
   data :- TCamera]
  (let [record (query state :create<! data)]
    (get-specific state (int (:1 record)))))

(sch/defn update!
  [state :- State
   id :- sch/Num
   data :- TCamera]
  (query state :update! (merge data {:camera-id id}))
  (get-specific state id))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (let [fs (media/get-all-files-by-camera state id)]
    (query state :delete! {:camera-id id})
    (media/delete-files! state fs))
  nil)

(sch/defn get-or-create! :- Camera
  [state :- State
   data :- TCamera]
  (or (get-specific-by-name state (select-keys data [:camera-name]))
      (create! state data)))

(sch/defn set-camera-status!
  [state :- State
   cam-id :- sch/Int
   cam-status :- sch/Int]
  (query state :set-camera-status!
    {:camera-id cam-id
     :camera-status-id cam-status}))

(defn make-available
  "Set the associated cameras' status to 'available'.
  `cameras' is a coll of camera IDs."
  [state cameras]
  (let [available-status (camera-status/available-status-id state)]
    (doseq [cam-id (distinct cameras)]
      (set-camera-status! state cam-id available-status))))

(s/def ::camera-id int?)

(s/fdef make-available
        :args (s/cat :state ::sysspec/state
                     :cameras (s/coll-of ::camera-id))
        :ret nil?)
