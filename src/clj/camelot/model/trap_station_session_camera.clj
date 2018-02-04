(ns camelot.model.trap-station-session-camera
  (:require
   [schema.core :as s]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]))

(def query (db/with-db-keys :trap-station-session-cameras))

(s/defrecord TrapStationSessionCamera
    [trap-station-session-camera-id :- s/Int
     trap-station-session-camera-created :- org.joda.time.DateTime
     trap-station-session-camera-updated :- org.joda.time.DateTime
     camera-id :- s/Int
     trap-station-session-id :- s/Int
     trap-station-session-camera-media-unrecoverable :- s/Bool
     trap-station-session-camera-import-path :- (s/maybe s/Str)
     camera-name :- (s/maybe s/Str)]
  {s/Any s/Any})

(s/defrecord TTrapStationSessionCamera
    [camera-id :- s/Int
     trap-station-session-id :- s/Int
     trap-station-session-camera-media-unrecoverable :- s/Bool
     trap-station-session-camera-import-path :- (s/maybe s/Str)]
  {s/Any s/Any})

(defn trap-station-session-camera
  [ks]
  (map->TrapStationSessionCamera
   (update ks :trap-station-session-camera-media-unrecoverable
           #(or % false))))

(defn ttrap-station-session-camera
  [ks]
  (map->TTrapStationSessionCamera
   (update ks :trap-station-session-camera-media-unrecoverable
           #(or % false))))

(s/defn get-all :- [TrapStationSessionCamera]
  [state :- State
   id :- s/Int]
  (->> {:trap-station-session-id id}
       (query state :get-all)
       (map trap-station-session-camera)))

(s/defn get-all* :- [TrapStationSessionCamera]
  [state :- State]
  (map trap-station-session-camera (query state :get-all* {})))

(s/defn get-specific :- (s/maybe TrapStationSessionCamera)
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-session-camera-id id}
           (query state :get-specific )
           first
           trap-station-session-camera))

(s/defn get-specific-with-camera-and-session :- (s/maybe TrapStationSessionCamera)
  "Return a session camera given a camera and session ID."
  [state :- State
   camera-id :- s/Int
   session-id :- s/Int]
  (some->> {:trap-station-session-id session-id
            :camera-id camera-id}
           (query state :get-specific-with-camera-and-session)
           first
           trap-station-session-camera))

(s/defn get-specific-by-import-path :- (s/maybe TrapStationSessionCamera)
  [state :- State
   path :- s/Str]
  (some->> {:trap-station-session-camera-import-path path}
           (query state :get-specific-by-import-path)
           (first)
           (trap-station-session-camera)))

(defn- camera-available?
  [state data]
  (let [tid (int (:trap-station-session-id data))
        active-sess (trap-station-session/get-active state tid)]
    (not-any? #(= % (:camera-id data)) active-sess)))

(s/defn create!* :- TrapStationSessionCamera
  "Create without checking camera availability."
  [state :- State
   data :- TTrapStationSessionCamera]
  (let [record (query state :create<! data)]
    (trap-station-session-camera (get-specific state (int (:1 record))))))

(s/defn create! :- TrapStationSessionCamera
  [state :- State
   data :- TTrapStationSessionCamera]
  {:pre [(camera-available? state data)]}
  (create!* state data))

(defn- camera-available-for-update?
  [state id data]
  (let [tid (int (:trap-station-session-id data))
        active-sess (trap-station-session/get-active state tid id)]
    (not-any? #(= % (:camera-id data)) active-sess)))

(s/defn update! :- TrapStationSessionCamera
  [state :- State
   id :- s/Int
   data :- TTrapStationSessionCamera]
  {:pre [(camera-available-for-update? state id data)]}
  (query state :update!
    (merge data {:trap-station-session-camera-id id}))
  (trap-station-session-camera (get-specific state id)))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-trap-station-session-camera state id)
        ps {:trap-station-session-camera-id id}
        cams (get-active-cameras state ps)]
    (query state :delete! ps)
    (media/delete-files! state fs)
    (camera/make-available state cams))
  nil)

(s/defn delete-media!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-trap-station-session-camera state id)]
    (query state :delete-media! {:trap-station-session-camera-id id})
    (media/delete-files! state fs))
  nil)

(s/defn get-available
  "Return the available cameras, factoring in whether they're in use elsewhere."
  [state :- State
   id :- s/Int]
  (query state :get-available))

(s/defn get-alternatives
  "Return the current and alternative cameras, factoring in whether they're in
  use elsewhere."
  [state :- State
   id :- s/Int]
  (let [res (get-specific state id)]
    (some->> res
             (query state :get-alternatives))))

(s/defn get-or-create-with-camera-and-session! :- TrapStationSessionCamera
  [state :- State
   data :- TTrapStationSessionCamera]
  (or (get-specific-with-camera-and-session
       state (:camera-id data) (:trap-station-session-id data))
      (create!* state data)))

(s/defn get-or-create! :- TrapStationSessionCamera
  [state :- State
   data :- TTrapStationSessionCamera]
  (or (get-specific-by-import-path
       state (:trap-station-session-camera-import-path data))
      (create! state data)))

(s/defn update-media-unrecoverable! :- TrapStationSessionCamera
  "Set the media recoverable flag for a given camera and session.  Returns the
  updated session camera."
  [state :- State
   camera-id :- s/Int
   trap-station-session-id :- s/Int
   media-unrecoverable :- s/Bool]
  (query state :update-media-unrecoverable!
    {:camera-id camera-id
     :trap-station-session-id trap-station-session-id
     :trap-station-session-camera-media-unrecoverable media-unrecoverable})
  (get-specific-with-camera-and-session state camera-id
                                        trap-station-session-id))
