(require '[camelot.util.db :as db])
(require '[camelot.util.state :as state])

(sql/defqueries "sql/migration-helpers/021.sql")

(defn- -m021-delete-taxonomies
  [state]
  (db/with-transaction [s state]
    (let [conn (state/lookup-connection s)
          taxonomies (-get-all {} conn)]
      (dorun (map #(-delete! {:taxonomy_id %} conn) taxonomies)))))

(let [system-config (state/system-config)
      system-state (state/config->state system-config)]
  (dorun (state/map-datasets -m021-delete-taxonomies system-state)))
