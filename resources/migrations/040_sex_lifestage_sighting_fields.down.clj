(require '[yesql.core :as sql])
(require '[clojure.java.jdbc :as jdbc])

(sql/defqueries "sql/migration-helpers/040.sql")

(defn- -m040-get-migrated-sighting-fields
  [conn]
  (let [q {:sighting_field_keys ["lifestage" "sex"]}]
    (map :sighting_field_id (-get-migrated-sighting-fields q conn))))

(defn- -m040-downgrade
  [conn]
  (jdbc/with-db-transaction [tx conn]
    (let [conn {:connection tx}]
      (doseq [sighting-field (-m040-get-migrated-sighting-fields conn)]
        (-delete-field! {:sighting_field_id sighting-field} conn)))))

(-m040-downgrade camelot.migration/*connection*)
