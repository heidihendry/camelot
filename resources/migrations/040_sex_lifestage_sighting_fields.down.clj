(require '[yesql.core :as sql])
(require '[camelot.util.db :as db])
(require '[camelot.state.datasets :as datasets])

(sql/defqueries "sql/migration-helpers/040.sql")

(defn- -m040-get-migrated-sighting-fields
  [conn]
  (let [q {:sighting_field_keys ["lifestage" "sex"]}]
    (map :sighting_field_id (-get-migrated-sighting-fields q conn))))

(defn- -m040-downgrade
  [state]
  (db/with-transaction [s state]
    (let [conn {:connection (datasets/lookup-connection (:datasets s))}]
      (doseq [sighting-field (-m040-get-migrated-sighting-fields conn)]
        (-delete-field! {:sighting_field_id sighting-field} conn)))))

(-m040-downgrade camelot.system.db.core/*migration-state*)
