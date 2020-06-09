(require '[yesql.core :as sql])
(require '[clojure.java.jdbc :as jdbc])
(require '[schema.core :as s])

(sql/defqueries "sql/migration-helpers/030.sql")

(defn- -m030-downgrade
  [conn]
  (jdbc/with-db-transaction [tx conn]
    (let [conn {:connection tx}]
      (doseq [st (-get-all-survey-taxonomy {} conn)]
        (-delete! {:survey_taxonomy_id st} conn)))))

(-m030-downgrade camelot.migration/*connection*)
