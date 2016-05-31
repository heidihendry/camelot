(ns camelot.handler.surveys
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.model.survey :refer [Survey SurveyCreate]]))

(sql/defqueries "sql/surveys.sql" {:connection db/spec})

(s/defn get-all :- [Survey]
  [state]
  (db/clj-keys (db/with-connection (:connection state) -get-all)))

(s/defn get-specific :- Survey
  [state
   id :- s/Num]
  (first (db/with-db-keys state -get-specific {:survey-id id})))

(s/defn get-specific-by-name :- Survey
  [state
   data :- {:survey-name s/Str}]
  (db/with-db-keys state -get-specific-by-name data))

(s/defn create!
  [state
   data :- SurveyCreate]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   id :- s/Num
   data :- Survey]
  (db/with-db-keys state -update! (merge data {:survey-id id}))
  (get-specific state (:survey-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys state -delete! {:survey-id id}))

(def routes
  (context "/surveys" []
           (GET "/" [] (rest/list-resources get-all :survey))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
