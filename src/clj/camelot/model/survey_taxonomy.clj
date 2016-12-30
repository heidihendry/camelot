(ns camelot.model.survey-taxonomy
  (:require
   [yesql.core :as sql]
   [camelot.system.state :refer [State]]
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.translation.core :as tr]))

;; NOTE: 030_survey_taxonomy_migration needs to be updated should this model
;; change.

(sql/defqueries "sql/survey-taxonomy.sql")

(s/defrecord TSurveyTaxonomy
    [survey-id :- s/Int
     taxonomy-id :- s/Int]
  {s/Any s/Any})

(s/defrecord SurveyTaxonomy
    [survey-taxonomy-id :-  s/Int
     survey-taxonomy-created :- org.joda.time.DateTime
     survey-taxonomy-updated :- org.joda.time.DateTime
     survey-id :- s/Int
     taxonomy-id :- s/Int]
  {s/Any s/Any})

(def survey-taxonomy map->SurveyTaxonomy)
(def tsurvey-taxonomy map->TSurveyTaxonomy)

(s/defn get-all :- [SurveyTaxonomy]
  "Retrieve all available survey taxonomies."
  [state :- State]
  (->> (db/with-connection state -get-all)
       (db/clj-keys)
       (map survey-taxonomy)))

(s/defn get-all-for-survey :- [SurveyTaxonomy]
  "Retrieve all entries for a given survey."
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
       (map survey-taxonomy)
       first))

(s/defn create! :- SurveyTaxonomy
  [state :- State
   data :- TSurveyTaxonomy]
  (let [record (db/with-db-keys state -create<! data)]
    (survey-taxonomy (get-specific state (int (:1 record))))))

(s/defn get-or-create! :- SurveyTaxonomy
  [state :- State
   data :- TSurveyTaxonomy]
  (or (get-specific-by-relations state (:survey-id data) (:taxonomy-id data))
      (create! state data)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -delete! {:survey-taxonomy-id id}))
