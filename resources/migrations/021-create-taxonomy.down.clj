(require '[yesql.core :as sql])
(require '[clojure.java.jdbc :as jdbc])

(sql/defqueries "sql/migration-helpers/021.sql")

(defn- -m021-delete-taxonomies
  [conn]
  (jdbc/with-db-transaction [tx conn]
    (let [conn {:connection tx}
          taxonomies (-get-all {} conn)]
      (dorun (map #(-delete! {:taxonomy_id %} conn) taxonomies)))))

(-m021-delete-taxonomies camelot.migration/*connection*)
