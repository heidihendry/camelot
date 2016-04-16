(ns camelot.model.surveys
  (:require [camelot.db :as db]
            [yesql.core :as sql]))

(sql/defqueries "surveys.sql" {:connection db/spec})
