(require '[yesql.core :as sql])
(require '[camelot.app.state :refer [State] :as state])
(require '[clojure.java.jdbc :as jdbc])
(require '[schema.core :as s])
(require '[camelot.db.core :as db])
(require '[camelot.util.config :as config])
(require '[camelot.db.survey-taxonomy :as survey-taxonomy])

(db/with-transaction [s (state/gen-state (config/config))]
  (doseq [st (survey-taxonomy/get-all s)]
    (survey-taxonomy/delete! s (:survey-taxonomy-id st))))
