(require '[yesql.core :as sql])
(require '[camelot.system.state :as state])
(require '[clojure.java.jdbc :as jdbc])
(require '[schema.core :as s])
(require '[camelot.util.db :as db])

(sql/defqueries "sql/migration-helpers/030.sql")

(db/with-transaction [s {:database {:connection state/spec}}]
  (let [conn (select-keys (:database s) [:connection])]
    (doseq [st (-get-all-survey-taxonomy {} conn)]
      (-delete! {:survey_taxonomy_id st} conn))))
