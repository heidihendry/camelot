(ns camelot.model.trap-station-session
  (:require [schema.core :as s]
            [camelot.db :as db]
            [clj-time.format :as tf]
            [camelot.model.state :refer [State]]
            [yesql.core :as sql]))

(sql/defqueries "sql/trap-station-sessions.sql" {:connection db/spec})

(s/defrecord TTrapStationSession
    [trap-station-id :- s/Int
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- org.joda.time.DateTime
     trap-station-session-notes :- (s/maybe s/Str)])

(s/defrecord TrapStationSession
    [trap-station-session-id :- s/Int
     trap-station-session-created :- org.joda.time.DateTime
     trap-station-session-updated :- org.joda.time.DateTime
     trap-station-id :- s/Int
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- org.joda.time.DateTime
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

(s/defn get-specific-by-dates :- (s/maybe TrapStationSession)
  [state :- State
   data :- TTrapStationSession]
  (some->> data
           (db/with-db-keys state -get-specific-by-dates)
           (first)
           (add-label)
           (trap-station-session)))

(s/defn create! :- TrapStationSession
  [state :- State
   data :- TTrapStationSession]
  (let [record (db/with-db-keys state -create<! data)]
    (trap-station-session (get-specific state (int (:1 record))))))

(s/defn update!
  "Update the value, dissoc'ing the label, as it's a computed field."
  [state :- State
   id :- s/Num
   data :- TTrapStationSession]
  (let [data (dissoc data :trap-station-session-label)]
    (db/with-db-keys state -update! (merge data {:trap-station-session-id id}))
    (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Num]
  (db/with-db-keys state -delete! {:trap-station-session-id id}))

(s/defn get-or-create! :- TrapStationSession
  [state :- State
   data :- TTrapStationSession]
  (or (get-specific-by-dates state data)
      (create! state data)))
