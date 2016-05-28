(ns camelot.handler.trap-station-sessions
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.trap-station-session :refer
             [TrapStationSession TrapStationSessionCreate TrapStationSessionLabeled]]
            [clj-time.format :as tf]))

(sql/defqueries "sql/trap-station-sessions.sql" {:connection db/spec})

(def date-formatter (tf/formatter "yyyy-MM-dd"))

(defn build-label
  [start end]
  (let [sp (tf/unparse date-formatter start)
        ep (tf/unparse date-formatter end)]
    (format "%s to %s" sp ep)))

(defn add-label
  "Assoc a key for the label, which is a computed value."
  [rec]
  (assoc rec :trap-station-session-label
         (build-label (:trap-station-session-start-date rec)
                      (:trap-station-session-end-date rec))))

(s/defn get-all :- [TrapStationSessionLabeled]
  [state id]
  (map add-label (db/with-db-keys -get-all {:trap-station-id id})))

(s/defn get-specific :- TrapStationSessionLabeled
  [state
   id :- s/Num]
  (add-label (first (db/with-db-keys -get-specific {:trap-station-session-id id}))))

(s/defn get-specific-by-dates :- (s/maybe TrapStationSessionLabeled)
  [state
   data]
  (let [result (first (db/with-db-keys -get-specific-by-dates data))]
    (when-not (or (nil? result) (empty? result))
      (add-label result))))

(s/defn create!
  [state
   data :- TrapStationSessionCreate]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  "Update the value, dissoc'ing the label, as it's a computed field."
  [state
   id :- s/Num
   data :- TrapStationSessionLabeled]
  (let [data (dissoc data :trap-station-session-label)]
    (db/with-db-keys -update! (merge data {:trap-station-session-id id}))
    (get-specific state (:trap-station-session-id data))))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:trap-station-session-id id}))

(def routes
  (context "/trap-station-sessions" []
           (GET "/trap-station/:id" [id]
                (rest/list-resources get-all :trap-station-session id))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
