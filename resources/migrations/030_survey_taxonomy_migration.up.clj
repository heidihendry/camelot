(require '[clojure.math.combinatorics :as combinatorics])
(require '[yesql.core :as sql])
(require '[camelot.model.state :refer [State]])
(require '[clojure.java.jdbc :as jdbc])
(require '[schema.core :as s])

(s/defn -m030-survey-ids :- [s/Int]
  [state :- State]
  (map :survey-id (camelot.model.survey/get-all state)))

(s/defn -m030-taxonomy-ids :- [s/Int]
  [state :- State]
  (map :taxonomy-id (camelot.model.taxonomy/get-all state)))

(s/defn -m030-all-pairs :- [[s/Int]]
  [state :- State]
  (let [ss (-m030-survey-ids state)
        ts (-m030-taxonomy-ids state)]
    (combinatorics/cartesian-product ss ts)))

(s/defn -m030-->survey-taxonomy
  [[survey taxonomy]]
  (camelot.model.survey-taxonomy/tsurvey-taxonomy {:survey-id survey
                                     :taxonomy-id taxonomy}))

(camelot.db/with-transaction
  [s (camelot.application/gen-state (camelot.util.config/config))]
  (doseq [p (-m030-all-pairs s)]
    (camelot.model.survey-taxonomy/create! s (-m030-->survey-taxonomy p))))
