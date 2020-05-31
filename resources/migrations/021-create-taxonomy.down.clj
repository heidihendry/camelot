(require '[camelot.util.db :as db])
(require '[camelot.state.datasets :as datasets])

(sql/defqueries "sql/migration-helpers/021.sql")

(defn- -m021-delete-taxonomies
  [state]
  (db/with-transaction [s state]
    (let [conn {:connection (datasets/lookup-connection (:datasets s))}
          taxonomies (-get-all {} conn)]
      (dorun (map #(-delete! {:taxonomy_id %} conn) taxonomies)))))

(-m021-delete-taxonomies camelot.system.db.core/*migration-state*)
