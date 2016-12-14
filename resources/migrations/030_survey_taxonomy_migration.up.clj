(require '[clojure.math.combinatorics :as combinatorics])
(require '[yesql.core :as sql])
(require '[camelot.db.core :as db])
(require '[camelot.app.state :refer [State] :as state])
(require '[clojure.java.jdbc :as jdbc])
(require '[schema.core :as s])
(require '[camelot.db.core :as db])
(require '[camelot.db.survey-taxonomy :as survey-taxonomy])

(sql/defqueries "sql/migration-helpers/030.sql" {:connection state/spec})

(s/defn -m030-survey-ids :- [s/Int]
  [state :- State]
  (map :survey_id (-get-all-survey)))

(s/defn -m030-taxonomy-ids :- [s/Int]
  [state :- State]
  (map :taxonomy_id (-get-all-taxonomy)))

(s/defn -m030-all-pairs :- [[s/Int]]
  [state :- State]
  (let [ss (-m030-survey-ids state)
        ts (-m030-taxonomy-ids state)]
    (combinatorics/cartesian-product ss ts)))

(s/defn -m030-->survey-taxonomy
  [[survey taxonomy]]
  (survey-taxonomy/tsurvey-taxonomy {:survey-id survey
                                     :taxonomy-id taxonomy}))

(db/with-transaction
  [s (state/gen-state*)]
  (doseq [p (-m030-all-pairs s)]
    (survey-taxonomy/create! s (-m030-->survey-taxonomy p))))
