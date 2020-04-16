(require '[yesql.core :as sql])
(require '[camelot.util.db :as db])
(require '[camelot.util.state :as state])

(sql/defqueries "sql/migration-helpers/040.sql")

(defn- -m040-get-migrated-sighting-fields
  [conn]
  (let [q {:sighting_field_keys ["lifestage" "sex"]}]
    (map :sighting_field_id (-get-migrated-sighting-fields q conn))))

(defn- -m040-downgrade
  [state]
  (db/with-transaction [s state]
    (let [conn (state/lookup-connection s)]
      (doseq [sighting-field (-m040-get-migrated-sighting-fields conn)]
        (-delete-field! {:sighting_field_id sighting-field} conn)))))

(let [system-config (state/system-config)
      system-state (state/config->state system-config)]
  (dorun (state/map-datasets -m040-downgrade system-state)))
