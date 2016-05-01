(ns camelot.handler.survey-sites
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.survey-site :refer [SurveySite SurveySiteCreate]]))

(sql/defqueries "sql/survey-sites.sql" {:connection db/spec})

(s/defn get-all :- [SurveySite]
  [state]
  (db/clj-keys (-get-all)))

(s/defn get-specific :- SurveySite
  [state
   id :- s/Num]
  (first (db/with-db-keys -get-specific {:survey-site-id id})))

(s/defn create!
  [state
   data :- SurveySiteCreate]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   data :- SurveySite]
  (db/with-db-keys -update! data)
  (get-specific state (:survey-site-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:survey-site-id id}))
