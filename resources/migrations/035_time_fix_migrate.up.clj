(require '[camelot.util.state :as state])
(require '[camelot.util.db :as db])
(require '[yesql.core :as sql])
(require '[clj-time.core :as t])
(require '[clj-time.coerce :as tc])

(sql/defqueries "sql/migration-helpers/035.sql")

(def all-keys
  (into {}
        (map #(hash-map % nil)
             [:survey_created_ms :survey_updated_ms
              :site_created_ms :site_updated_ms
              :survey_site_created_ms :survey_site_updated_ms
              :trap_station_created_ms :trap_station_updated_ms
              :trap_station_session_created_ms :trap_station_session_updated_ms
              :trap_station_session_start_date_ms :trap_station_session_end_date_ms
              :trap_station_session_camera_created_ms :trap_station_session_camera_updated_ms
              :camera_created_ms :camera_updated_ms
              :taxonomy_created_ms :taxonomy_updated_ms
              :survey_taxonomy_created_ms :survey_taxonomy_updated_ms
              :survey_file_created_ms :survey_file_updated_ms
              :media_created_ms :media_updated_ms :media_capture_timestamp_ms
              :sighting_created_ms :sighting_updated_ms
              :photo_created_ms :photo_updated_ms])))

(defn migrate-table!
  [conn get-fn migrate-fn]
  (let [s (get-fn {} conn)]
    (dorun (map #(migrate-fn (merge all-keys (reduce-kv (fn [acc k v]
                                                          (if (instance? java.sql.Timestamp v)
                                                            (assoc acc k (tc/from-sql-time v)
                                                                   (keyword (str (name k) "_ms"))
                                                                   (tc/to-long (tc/from-sql-time v)))
                                                            (assoc acc k v))) {} %))
                             conn)
                s))))

(db/with-transaction [s {:database {:connection state/spec}}]
  (let [conn (select-keys (:database s) [:connection])]
    (migrate-table! conn -get-surveys -migrate-survey!)
    (migrate-table! conn -get-sites -migrate-site!)
    (migrate-table! conn -get-survey-sites -migrate-survey-site!)
    (migrate-table! conn -get-trap-stations -migrate-trap-station!)
    (migrate-table! conn -get-trap-station-sessions -migrate-trap-station-session!)
    (migrate-table! conn -get-trap-station-session-cameras -migrate-trap-station-session-camera!)
    (migrate-table! conn -get-cameras -migrate-camera!)
    (migrate-table! conn -get-taxonomies -migrate-taxonomy!)
    (migrate-table! conn -get-survey-taxonomies -migrate-survey-taxonomy!)
    (migrate-table! conn -get-survey-files -migrate-survey-file!)
    (migrate-table! conn -get-media -migrate-media!)
    (migrate-table! conn -get-sightings -migrate-sighting!)
    (migrate-table! conn -get-photos -migrate-photo!))
  nil)
