(require '[clojure.java.jdbc :as jdbc])
(require '[clojure.math.combinatorics :as combinatorics])
(require '[yesql.core :as sql])
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
  [conn]
  (jdbc/with-db-transaction [tx conn]
    ;; TODO #217 and all like this
    (let [conn {:connection tx}]
      (doseq [p (-m030-all-pairs conn)]
        (-create<! (-m030-->survey-taxonomy p) conn)))))

(-m030-upgrade camelot.migration/*connection*)
