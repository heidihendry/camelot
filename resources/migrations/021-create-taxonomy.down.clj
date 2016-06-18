(ns camelot.migration.create-taxonomy-down
  (:require [camelot.model.taxonomy :as taxonomy]
            [yesql.core :as sql]
            [camelot.db :as db]
            [clojure.string :as str]
            [camelot.application :as app]
            [camelot.util.config :as config]
            [camelot.model.species :as species]))

(defn delete-taxonomies
  []
  (let [state (app/gen-state (config/config))]
    (db/with-transaction [s state]
      (let [taxonomies (taxonomy/get-all s)]
        (dorun (map #(taxonomy/delete! s (:taxonomy-id %)) taxonomies))))))

(delete-taxonomies)
