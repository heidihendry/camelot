(ns camelot.handler.surveys
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.survey :refer [Survey SurveyCreate]]))

(sql/defqueries "sql/surveys.sql" {:connection db/spec})

(s/defn get-all :- [Survey]
  [state]
  (db/clj-keys (-get-all)))

(s/defn get-specific :- Survey
  [state id]
  (first (db/with-db-keys -get-specific {:survey-id id})))

(s/defn get-specific-by-name :- Survey
  [state
   data :- {:survey-name s/Str}]
  (db/with-db-keys -get-specific-by-name data))

(s/defn create!
  [state
   data :- SurveyCreate]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   data :- Survey]
  (db/with-db-keys -update! data)
  (get-specific state (:survey-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:survey-id id}))
