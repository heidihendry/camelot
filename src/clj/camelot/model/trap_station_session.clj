(ns camelot.model.trap-station-session
  (:require
   [schema.core :as s]
   [camelot.util.db :as db]
   [clj-time.format :as tf]
   [camelot.spec.schema.state :refer [State]]
   [clj-time.core :as t]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]
   [camelot.translation.core :as tr]))

(def query (db/with-db-keys :trap-station-sessions))

(s/defrecord TTrapStationSession
    [trap-station-id :- s/Int
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- (s/maybe org.joda.time.DateTime)
     trap-station-session-notes :- (s/maybe s/Str)]
  {s/Any s/Any})

(s/defrecord TrapStationSession
    [trap-station-session-id :- s/Int
     trap-station-session-created :- org.joda.time.DateTime
     trap-station-session-updated :- org.joda.time.DateTime
     trap-station-id :- s/Int
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- (s/maybe org.joda.time.DateTime)
     trap-station-session-notes :- (s/maybe s/Str)
     trap-station-session-label :- (s/maybe s/Str)]
  {s/Any s/Any})

(def trap-station-session map->TrapStationSession)
(def ttrap-station-session map->TTrapStationSession)

(def date-formatter (tf/formatter "yyyy-MM-dd"))

(defn- build-label
  [state start end]
  (let [sp (tf/unparse date-formatter start)]
    (if end
      (let [ep (tf/unparse date-formatter end)]
        (tr/translate state ::trap-station-session-closed-label sp ep))
      (tr/translate state ::trap-station-session-ongoing-label sp))))

(defn- add-label
  "Assoc a key for the label, which is a computed value."
  [state rec]
  (assoc rec :trap-station-session-label
         (build-label state
                      (:trap-station-session-start-date rec)
                      (:trap-station-session-end-date rec))))

(s/defn get-all :- [TrapStationSession]
  [state :- State
   id :- s/Int]
  (->> {:trap-station-id id}
       (query state :get-all)
       (map #(add-label state %))
       (map trap-station-session)))

(s/defn get-all* :- [TrapStationSession]
  [state :- State]
  (->> (query state :get-all* {})
       (map #(add-label state %))
       (map trap-station-session)))

(s/defn get-specific :- TrapStationSession
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-session-id id}
           (query state :get-specific)
           (first)
           (add-label state)
           (trap-station-session)))

(defn get-active
  "Return cameras which are active over the time range of the session with the given id."
  ([state session-id]
   (let [session (get-specific state session-id)]
     (when session
       (map :camera-id (query state :get-active session)))))
  ([state session-id session-camera-id]
   (let [session (get-specific state session-id)]
     (when session
       (->> session
            (query state :get-active)
            (remove #(= (:trap-station-session-camera-id %) session-camera-id))
            (map :camera-id))))))

(s/defn get-specific-by-dates :- (s/maybe TrapStationSession)
  [state :- State
   data :- TTrapStationSession]
  (some->> data
           (query state :get-specific-by-dates)
           (first)
           (add-label state)
           (trap-station-session)))

(s/defn get-specific-by-trap-station-session-camera-id :- (s/maybe TrapStationSession)
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-session-camera-id id}
           (query state :get-specific-by-trap-station-session-camera-id)
           (first)
           (trap-station-session)))

(defn- start-date-before-end-date?
  [data]
  (let [start (:trap-station-session-start-date data)
        end(:trap-station-session-end-date data)]
    (or (nil? end) (= start end) (t/before? start end))))

(s/defn create! :- TrapStationSession
  [state :- State
   data :- TTrapStationSession]
  {:pre [(start-date-before-end-date? data)]}
  (let [record (query state :create<! data)]
    (trap-station-session (get-specific state (int (:1 record))))))

(s/defn update!
  "Update the value, dissoc'ing the label, as it's a computed field."
  [state :- State
   id :- s/Int
   data :- TTrapStationSession]
  {:pre [(start-date-before-end-date? data)]}
  (db/with-transaction [s state]
    (let [data (dissoc data :trap-station-session-label)]
      (query s :update! (merge data {:trap-station-session-id id}))
      (get-specific s id))))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-trap-station-session state id)
        ps {:trap-station-session-id id}
        cams (get-active-cameras state ps)]
    (query state :delete! ps)
    (media/delete-files! state fs)
    (camera/make-available state cams))
  nil)

(s/defn get-or-create! :- TrapStationSession
  [state :- State
   data :- TTrapStationSession]
  (or (get-specific-by-dates state data)
      (create! state data)))

(s/defn set-session-end-date!
  [state :- State
   data]
  (query state :set-session-end-date! data))
