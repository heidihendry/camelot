(require '[yesql.core :as sql])
(require '[camelot.model.state :refer [State]])
(require '[clojure.java.jdbc :as jdbc])
(require '[schema.core :as s]))

(camelot.db/with-transaction [s (camelot.application/gen-state (camelot.util.config/config))]
  (doseq [st (camelot.model.survey-taxonomy/get-all s)]
    (camelot.model.survey-taxonomy/delete! s (:survey-taxonomy-id st))))
