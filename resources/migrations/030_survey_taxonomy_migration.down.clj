(ns migrations.survey-taxonomy-migration-down
  "Remove all survey-taxonomy records."
  (:require [yesql.core :as sql]
            [camelot.db :as db]
            [camelot.model.state :refer [State]]
            [camelot.model.survey-taxonomy :as survey-taxonomy]
            [camelot.application :as app]
            [camelot.util.config :as config]
            [clojure.java.jdbc :as jdbc]
            [schema.core :as s]))

(db/with-transaction [s (app/gen-state (config/config))]
  (doseq [st (survey-taxonomy/get-all s)]
    (survey-taxonomy/delete! s (:survey-taxonomy-id st))))
