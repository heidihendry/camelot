(require '[yesql.core :as sql])
(require '[camelot.util.db :as db])
(require '[camelot.util.state :as state])

(sql/defqueries "sql/migration-helpers/040.sql")

(defn- -m040-get-migrated-sighting-fields
  [conn]
  (let [q {:sighting_field_keys ["lifestage" "sex"]}]
    (map :sighting_field_id (-get-migrated-sighting-fields q conn))))

(db/with-transaction [s {:database {:connection state/spec}}]
  (let [conn (select-keys (:database s) [:connection])]
    (doseq [sighting-field (-m040-get-migrated-sighting-fields conn)]
      (-delete-field! {:sighting_field_id sighting-field} conn))))
