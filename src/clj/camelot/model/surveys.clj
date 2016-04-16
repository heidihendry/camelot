(ns camelot.model.surveys
  (:require [camelot.db :as db]))

(defqueries "surveys.sql" {:connection db/spec})
