(require '[clojure.math.combinatorics :as combinatorics])
(require '[yesql.core :as sql])
(require '[camelot.util.db :as db])
(require '[camelot.util.state :as state])
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
    ;; TODO and all like this
    (let [conn (state/lookup-connection s)]
      (doseq [p (-m030-all-pairs conn)]
        (-create<! (-m030-->survey-taxonomy p) conn)))))

(let [system-config (state/system-config)
      system-state (state/config->state system-config)]
  (dorun (state/map-datasets -m030-upgrade system-state)))
