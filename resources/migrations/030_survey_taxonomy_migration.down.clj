(require '[yesql.core :as sql])
(require '[camelot.state.datasets :as datasets])
(require '[clojure.java.jdbc :as jdbc])
(require '[schema.core :as s])
(require '[camelot.util.db :as db])

(sql/defqueries "sql/migration-helpers/030.sql")

(defn- -m030-downgrade
  [state]
  (db/with-transaction [s state]
    (let [conn {:connection (datasets/lookup-connection (:datasets s))}]
      (doseq [st (-get-all-survey-taxonomy {} conn)]
        (-delete! {:survey_taxonomy_id st} conn)))))

(-m030-downgrade camelot.system.db.core/*migration-state*)
