(require '[clojure.math.combinatorics :as combinatorics])
(require '[yesql.core :as sql])
(require '[camelot.util.db :as db])
(require '[camelot.state.datasets :as datasets])
(require '[schema.core :as s])

(sql/defqueries "sql/migration-helpers/030.sql")

(s/defn -m030-survey-ids :- [s/Int]
  [conn]
  (map :survey_id (-get-all-survey {} conn)))

(s/defn -m030-taxonomy-ids :- [s/Int]
  [conn]
  (map :taxonomy_id (-get-all-taxonomy {} conn)))

(s/defn -m030-all-pairs :- [[s/Int]]
  [conn]
  (let [ss (-m030-survey-ids conn)
        ts (-m030-taxonomy-ids conn)]
    (combinatorics/cartesian-product ss ts)))

(s/defn -m030-->survey-taxonomy
  [[survey taxonomy]]
  {:survey_id survey
   :taxonomy_id taxonomy})

(defn- -m030-upgrade
  [state]
  (db/with-transaction [s state]
    ;; TODO #217 and all like this
    (let [conn {:connection (datasets/lookup-connection (:datasets s))}]
      (doseq [p (-m030-all-pairs conn)]
        (-create<! (-m030-->survey-taxonomy p) conn)))))

(-m030-upgrade camelot.system.db.core/*migration-state*)
