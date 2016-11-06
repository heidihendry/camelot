(require '[clojure.math.combinatorics :as combinatorics])
(require '[yesql.core :as sql])
(require '[camelot.model.state :refer [State]])
(require '[clojure.java.jdbc :as jdbc])
(require '[schema.core :as s])

(s/defn 030-survey-ids :- [s/Int]
  [state :- State]
  (map :survey-id (camelot.model.survey/get-all state)))

(s/defn 030-taxonomy-ids :- [s/Int]
  [state :- State]
  (map :taxonomy-id (camelot.model.taxonomy/get-all state)))

(s/defn 030-all-pairs :- [[s/Int]]
  [state :- State]
  (let [ss (030-survey-ids state)
        ts (030-taxonomy-ids state)]
    (combinatorics/cartesian-product ss ts)))

(s/defn 030-->survey-taxonomy
  [[survey taxonomy]]
  (camelot.model.survey-taxonomy/tsurvey-taxonomy {:survey-id survey
                                     :taxonomy-id taxonomy}))

(camelot.db/with-transaction
  [s (camelot.application/gen-state (camelot.util.config/config))]
  (doseq [p (030-all-pairs s)]
    (camelot.model.survey-taxonomy/create! s (030-->survey-taxonomy p))))
