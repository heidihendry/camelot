(ns migrations.survey-taxonomy-migration-up
  "Add all species to all surveys. Previously there was no relationship
  between species and surveys."
  (:require [clojure.math.combinatorics :as combinatorics]
            [yesql.core :as sql]
            [camelot.db :as db]
            [camelot.model.state :refer [State]]
            [camelot.model.survey :as survey]
            [camelot.model.taxonomy :as taxonomy]
            [camelot.model.survey-taxonomy :as survey-taxonomy]
            [camelot.application :as app]
            [camelot.util.config :as config]
            [clojure.java.jdbc :as jdbc]
            [schema.core :as s]))

(s/defn survey-ids :- [s/Int]
  [state :- State]
  (map :survey-id (survey/get-all state)))

(s/defn taxonomy-ids :- [s/Int]
  [state :- State]
  (map :taxonomy-id (taxonomy/get-all state)))

(s/defn all-pairs :- [[s/Int]]
  [state :- State]
  (let [ss (survey-ids state)
        ts (taxonomy-ids state)]
    (combinatorics/cartesian-product ss ts)))

(s/defn ->survey-taxonomy
  [[survey taxonomy]]
  (survey-taxonomy/tsurvey-taxonomy {:survey-id survey
                                     :taxonomy-id taxonomy}))

(db/with-transaction [s (app/gen-state (config/config))]
  (doseq [p (all-pairs s)]
    (survey-taxonomy/create! s (->survey-taxonomy p))))
