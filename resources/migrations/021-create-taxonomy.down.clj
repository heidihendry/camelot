(require '[camelot.db.taxonomy :as taxonomy])
(require '[camelot.db.core :as db])
(require '[camelot.app.state :as state])

(defn- -m021-delete-taxonomies
  []
  (let [state (state/gen-state)]
    (db/with-transaction [s state]
      (let [taxonomies (taxonomy/get-all s)]
        (dorun (map #(taxonomy/delete! s (:taxonomy-id %)) taxonomies))))))

(-m021-delete-taxonomies)
