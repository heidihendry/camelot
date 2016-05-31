(ns camelot.handler.trap-station-session-cameras
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.trap-station-session-camera :refer
             [TrapStationSessionCamera TrapStationSessionCameraCreate]]
            [camelot.handler.trap-station-sessions :as trap-station-sessions]))

(sql/defqueries "sql/trap-station-session-cameras.sql" {:connection db/spec})

(defn- get-active
  "Return cameras which are active over the time range of the session with the given id."
  [state session-id]
  (let [session (trap-station-sessions/get-specific state session-id)]
    (when session
      (db/with-db-keys state -get-active session))))

(s/defn get-all :- [TrapStationSessionCamera]
  [state id]
  (db/with-db-keys state -get-all {:trap-station-session-id id}))

(s/defn get-specific :- TrapStationSessionCamera
  [state
   id :- s/Num]
  (first (db/with-db-keys state -get-specific {:trap-station-session-camera-id id})))

(s/defn get-specific-by-camera :- (s/maybe TrapStationSessionCamera)
  [state
   data]
  (first (db/with-db-keys state -get-specific-by-camera data)))

(s/defn get-specific-by-import-path :- (s/maybe TrapStationSessionCamera)
  [state
   path]
  (first (db/with-db-keys state -get-specific-by-import-path {:trap-station-session-camera-import-path path})))

(s/defn create!
  [state
   data :- TrapStationSessionCameraCreate]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   id :- s/Num
   data :- TrapStationSessionCamera]
  (db/with-db-keys state -update! (merge data {:trap-station-session-camera-id id}))
  (get-specific state (:trap-station-session-camera-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys state -delete! {:trap-station-session-camera-id id}))

(s/defn get-available
  "Return the available cameras, factoring in whether they're in use elsewhere."
  [state id]
  (let [active (map :camera-id (get-active state id))]
    (->> {:trap-station-session-id id}
         (db/with-db-keys state -get-available)
         (remove #(some #{(:camera-id %)} active)))))

(s/defn get-alternatives
  "Return the current and alternative cameras, factoring in whether they're in use elsewhere."
  [state id]
  (let [res (get-specific state id)
        active (map :camera-id (get-active state (:trap-station-session-id res)))]
    (prn active)
    (->> res
         (db/with-db-keys state -get-alternatives)
         (remove #(and (some #{(:camera-id %)} active)
                       (not= (:camera-id res) (:camera-id %)))))))

(def routes
  (context "/trap-station-session-cameras" []
           (GET "/trap-station-session/:id" [id]
                (rest/list-resources get-all :trap-station-session-camera id))
           (GET "/available/:id" [id] (rest/list-available get-available id))
           (GET "/alternatives/:id" [id] (rest/list-available get-alternatives id))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
