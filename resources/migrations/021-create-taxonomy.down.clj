(require '[camelot.model.taxonomy :as taxonomy])
(require '[camelot.db :as db])
(require '[camelot.application :as app])
(require '[camelot.util.config :as config])

(defn 021-delete-taxonomies
  []
  (let [state (app/gen-state (config/config))]
    (db/with-transaction [s state]
      (let [taxonomies (taxonomy/get-all s)]
        (dorun (map #(taxonomy/delete! s (:taxonomy-id %)) taxonomies))))))

(021-delete-taxonomies)
