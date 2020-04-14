(ns camelot.model.trap-station-session-camera
  (:require
   [camelot.spec.system :as sysspec]
   [clojure.spec.alpha :as s]
   [schema.core :as sch]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]))

(def query (db/with-db-keys :trap-station-session-cameras))

(sch/defrecord TrapStationSessionCamera
    [trap-station-session-camera-id :- sch/Int
     trap-station-session-camera-created :- org.joda.time.DateTime
     trap-station-session-camera-updated :- org.joda.time.DateTime
     camera-id :- sch/Int
     trap-station-session-id :- sch/Int
     trap-station-session-camera-media-unrecoverable :- sch/Bool
     trap-station-session-camera-import-path :- (sch/maybe sch/Str)
     camera-name :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(sch/defrecord TTrapStationSessionCamera
    [camera-id :- sch/Int
     trap-station-session-id :- sch/Int
     trap-station-session-camera-media-unrecoverable :- sch/Bool
     trap-station-session-camera-import-path :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

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

(sch/defn get-all :- [TrapStationSessionCamera]
  [state :- State
   id :- sch/Int]
  (->> {:trap-station-session-id id}
       (query state :get-all)
       (map trap-station-session-camera)))

(sch/defn get-all* :- [TrapStationSessionCamera]
  [state :- State]
  (map trap-station-session-camera (query state :get-all* {})))

(sch/defn get-specific :- (sch/maybe TrapStationSessionCamera)
  [state :- State
   id :- sch/Int]
  (some->> {:trap-station-session-camera-id id}
           (query state :get-specific )
           first
           trap-station-session-camera))

(sch/defn get-specific-with-camera-and-session :- (sch/maybe TrapStationSessionCamera)
  "Return a session camera given a camera and session ID."
  [state :- State
   camera-id :- sch/Int
   session-id :- sch/Int]
  (some->> {:trap-station-session-id session-id
            :camera-id camera-id}
           (query state :get-specific-with-camera-and-session)
           first
           trap-station-session-camera))

(sch/defn get-specific-by-import-path :- (sch/maybe TrapStationSessionCamera)
  [state :- State
   path :- sch/Str]
  (some->> {:trap-station-session-camera-import-path path}
           (query state :get-specific-by-import-path)
           (first)
           (trap-station-session-camera)))

(defn- camera-available?
  [state data]
  (let [tid (int (:trap-station-session-id data))
        active-sess (trap-station-session/get-active state tid)]
    (not-any? #(= % (:camera-id data)) active-sess)))

(sch/defn create!* :- TrapStationSessionCamera
  "Create without checking camera availability."
  [state :- State
   data :- TTrapStationSessionCamera]
  (let [record (query state :create<! data)]
    (trap-station-session-camera (get-specific state (int (:1 record))))))

(sch/defn create! :- TrapStationSessionCamera
  [state :- State
   data :- TTrapStationSessionCamera]
  {:pre [(camera-available? state data)]}
  (create!* state data))

(defn- camera-available-for-update?
  [state id data]
  (let [tid (int (:trap-station-session-id data))
        active-sess (trap-station-session/get-active state tid id)]
    (not-any? #(= % (:camera-id data)) active-sess)))

(sch/defn update! :- TrapStationSessionCamera
  [state :- State
   id :- sch/Int
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

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (let [fs (media/get-all-files-by-trap-station-session-camera state id)
        ps {:trap-station-session-camera-id id}
        cams (get-active-cameras state ps)]
    (query state :delete! ps)
    (media/delete-files! state fs)
    (camera/make-available state cams))
  nil)

(sch/defn delete-media!
  [state :- State
   id :- sch/Int]
  (let [fs (media/get-all-files-by-trap-station-session-camera state id)]
    (query state :delete-media! {:trap-station-session-camera-id id})
    (media/delete-files! state fs))
  nil)

(sch/defn get-available
  "Return the available cameras, factoring in whether they're in use elsewhere."
  [state :- State
   id :- sch/Int]
  (query state :get-available))

(sch/defn get-alternatives
  "Return the current and alternative cameras, factoring in whether they're in
  use elsewhere."
  [state :- State
   id :- sch/Int]
  (let [res (get-specific state id)]
    (some->> res
             (query state :get-alternatives))))

(sch/defn get-or-create-with-camera-and-session! :- TrapStationSessionCamera
  [state :- State
   data :- TTrapStationSessionCamera]
  (or (get-specific-with-camera-and-session
       state (:camera-id data) (:trap-station-session-id data))
      (create!* state data)))

(sch/defn get-or-create! :- TrapStationSessionCamera
  [state :- State
   data :- TTrapStationSessionCamera]
  (or (get-specific-by-import-path
       state (:trap-station-session-camera-import-path data))
      (create! state data)))

(sch/defn update-media-unrecoverable! :- TrapStationSessionCamera
  "Set the media recoverable flag for a given camera and session.  Returns the
  updated session camera."
  [state :- State
   camera-id :- sch/Int
   trap-station-session-id :- sch/Int
   media-unrecoverable :- sch/Bool]
  (query state :update-media-unrecoverable!
    {:camera-id camera-id
     :trap-station-session-id trap-station-session-id
     :trap-station-session-camera-media-unrecoverable media-unrecoverable})
  (get-specific-with-camera-and-session state camera-id
                                        trap-station-session-id))

(s/def ::trap-station-session-summary
  (s/keys :req-un [::survey-id
                   ::trap-station-id
                   ::trap-station-name
                   ::camera-id
                   ::trap-station-session-id
                   ::trap-station-session-start-date]
          :opt-un [::trap-station-session-end-date]))

(s/def ::trap-station-session-summaries
  (s/coll-of ::trap-station-session-summary :kind vector?))

(s/def ::camera-usage
  (s/keys :req-un [::trap-station-session-summaries]))

(s/fdef get-camera-usage
  :args (s/cat :state ::sysspec/state
               :camera-id int?)
  :ret ::camera-usage
  :fn (fn [{:keys [ret]}]
        (let [summaries (:trap-station-session-summaries ret)]
          (and (coll? summaries)
               (let [start-times (map #(-> % :trap-station-session-start-date tc/to-long) summaries)]
                 (= start-times (sort start-times)))))))

(defn get-camera-usage
  "Return a chronology for the usage of a camera"
  [state camera-id]
  (->> {:camera-id camera-id}
       (query state :get-camera-usage)
       (sort-by :trap-station-session-start-date)
       (vec)
       (hash-map :trap-station-session-summaries)))
