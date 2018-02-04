(require '[camelot.util.db :as db])
(require '[camelot.util.state :as state])

(sql/defqueries "sql/migration-helpers/021.sql")

(defn- -m021-delete-taxonomies
  []
  (let [state {:database {:connection state/spec}}]
    (db/with-transaction [s state]
      (let [conn(select-keys (:database s) [:connection])
            taxonomies (-get-all {} conn)]
        (dorun (map #(-delete! {:taxonomy_id %} conn) taxonomies))))))

(-m021-delete-taxonomies)
