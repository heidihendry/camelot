(ns camelot.model.survey-taxonomy
  (:require [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [schema.core :as s]
            [camelot.db :as db]
            [camelot.translation.core :as tr]
            [camelot.application :as app]
            [camelot.util.config :as config]))

(sql/defqueries "sql/survey-taxonomy.sql" {:connection db/spec})

(s/defrecord TSurveyTaxonomy
    [survey-id :- s/Int
     taxonomy-id :- s/Int])

(s/defrecord SurveyTaxonomy
    [survey-taxonomy-id :-  s/Int
     survey-taxonomy-created :- org.joda.time.DateTime
     survey-taxonomy-updated :- org.joda.time.DateTime
     survey-id :- s/Int
     taxonomy-id :- s/Int])

(s/defn survey-taxonomy :- SurveyTaxonomy
  [{:keys [survey-taxonomy-id survey-taxonomy-created
           survey-taxonomy-updated survey-id taxonomy-id]}]
  (->SurveyTaxonomy survey-taxonomy-id survey-taxonomy-created
                    survey-taxonomy-updated survey-id taxonomy-id))

(s/defn tsurvey-taxonomy :- TSurveyTaxonomy
  [{:keys [survey-id taxonomy-id]}]
  (->TSurveyTaxonomy survey-id taxonomy-id))

(s/defn get-all :- [SurveyTaxonomy]
  "Retrieve all available survey taxonomies."
  [state :- State]
  (->> (db/with-connection (:connection state) -get-all)
       (db/clj-keys)
       (map survey-taxonomy)))

(s/defn get-all-for-survey :- [SurveyTaxonomy]
  "Retrieve all entries for a given survey"
  [state :- State
   survey-id :- s/Int]
  (->> {:survey-id survey-id}
       (db/with-db-keys state -get-all-for-survey)
       (map survey-taxonomy)))

(s/defn get-all-for-taxonomy :- [SurveyTaxonomy]
  "Retrieve all entries for a given taxonomy."
  [state :- State
   taxonomy-id :- s/Int]
  (->> {:taxonomy-id taxonomy-id}
       (db/with-db-keys state -get-all-for-taxonomy)
       (map survey-taxonomy)))

(s/defn get-specific :- (s/maybe SurveyTaxonomy)
  "Retrieve the entry with the given ID."
  [state :- State
   id :- s/Int]
  (->> {:survey-taxonomy-id id}
       (db/with-db-keys state -get-specific)
       (map survey-taxonomy)
       first))

(s/defn get-specific-by-relations :- (s/maybe SurveyTaxonomy)
  "Retrieve the survey taxonomy with the given survey and taxonomy ID."
  [state :- State
   survey-id :- s/Int
   taxonomy-id :- s/Int]
  (->> {:survey-id survey-id
        :taxonomy-id taxonomy-id}
       (db/with-db-keys state -get-specific-by-relations)
       (map survey-taxonomy)))

(s/defn create! :- SurveyTaxonomy
  [state :- State
   data :- TSurveyTaxonomy]
  (let [record (db/with-db-keys state -create<! data)]
    (survey-taxonomy (get-specific state (int (:1 record))))))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -delete! {:survey-taxonomy-id id}))
