(ns camelot.db.trap-station-session
  (:require
   [schema.core :as s]
   [camelot.db.core :as db]
   [clj-time.format :as tf]
   [camelot.app.state :refer [State]]
   [yesql.core :as sql]
   [clj-time.core :as t]
   [camelot.db.media :as media]))

(sql/defqueries "sql/trap-station-sessions.sql" {:connection db/spec})

(s/defrecord TTrapStationSession
    [trap-station-id :- s/Int
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- (s/maybe org.joda.time.DateTime)
     trap-station-session-notes :- (s/maybe s/Str)])

(s/defrecord TrapStationSession
    [trap-station-session-id :- s/Int
     trap-station-session-created :- org.joda.time.DateTime
     trap-station-session-updated :- org.joda.time.DateTime
     trap-station-id :- s/Int
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- (s/maybe org.joda.time.DateTime)
     trap-station-session-notes :- (s/maybe s/Str)
     trap-station-session-label :- (s/maybe s/Str)])

(s/defn trap-station-session :- TrapStationSession
  [{:keys [trap-station-session-id trap-station-session-created
           trap-station-session-updated trap-station-id
           trap-station-session-start-date trap-station-session-end-date
           trap-station-session-label trap-station-session-notes]}]
  (->TrapStationSession trap-station-session-id trap-station-session-created
           trap-station-session-updated trap-station-id
           trap-station-session-start-date trap-station-session-end-date
           trap-station-session-notes trap-station-session-label))

(s/defn ttrap-station-session :- TTrapStationSession
  [{:keys [trap-station-id trap-station-session-start-date
           trap-station-session-end-date trap-station-session-notes]}]
  (->TTrapStationSession trap-station-id trap-station-session-start-date
                        trap-station-session-end-date
                        trap-station-session-notes))

(def date-formatter (tf/formatter "yyyy-MM-dd"))

(defn- build-label
  [start end]
  (let [sp (tf/unparse date-formatter start)
        ep (tf/unparse date-formatter end)]
    (format "%s to %s" sp ep)))

(defn- add-label
  "Assoc a key for the label, which is a computed value."
  [rec]
  (assoc rec :trap-station-session-label
         (build-label (:trap-station-session-start-date rec)
                      (:trap-station-session-end-date rec))))

(s/defn get-all :- [TrapStationSession]
  [state :- State
   id :- s/Int]
  (->> {:trap-station-id id}
       (db/with-db-keys state -get-all)
       (map add-label)
       (map trap-station-session)))

(s/defn get-specific :- TrapStationSession
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-session-id id}
           (db/with-db-keys state -get-specific)
           (first)
           (add-label)
           (trap-station-session)))

(defn get-active
  "Return cameras which are active over the time range of the session with the given id."
  ([state session-id]
   (let [session (get-specific state session-id)]
     (when session
       (map :camera-id (db/with-db-keys state -get-active session)))))
  ([state session-id session-camera-id]
   (let [session (get-specific state session-id)]
     (when session
       (->> session
            (db/with-db-keys state -get-active)
            (remove #(= (:trap-station-session-camera-id %) session-camera-id))
            (map :camera-id))))))

(s/defn get-specific-by-dates :- (s/maybe TrapStationSession)
  [state :- State
   data :- TTrapStationSession]
  (some->> data
           (db/with-db-keys state -get-specific-by-dates)
           (first)
           (add-label)
           (trap-station-session)))

(s/defn get-specific-by-trap-station-session-camera-id :- (s/maybe TrapStationSession)
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-session-camera-id id}
           (db/with-db-keys state -get-specific-by-trap-station-session-camera-id)
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
  (let [record (db/with-db-keys state -create<! data)]
    (trap-station-session (get-specific state (int (:1 record))))))

(s/defn update!
  "Update the value, dissoc'ing the label, as it's a computed field."
  [state :- State
   id :- s/Int
   data :- TTrapStationSession]
  {:pre [(start-date-before-end-date? data)]}
  (db/with-transaction [s state]
    (let [data (dissoc data :trap-station-session-label)]
      (db/with-db-keys s -update! (merge data {:trap-station-session-id id}))
      (get-specific s id))))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-trap-station-session state id)]
    (db/with-db-keys state -delete! {:trap-station-session-id id})
    (media/delete-files! state fs))
  nil)

(s/defn get-or-create! :- TrapStationSession
  [state :- State
   data :- TTrapStationSession]
  (or (get-specific-by-dates state data)
      (create! state data)))

(s/defn set-session-end-date!
  [state :- State
   data]
  (db/with-db-keys state -set-session-end-date! data))
