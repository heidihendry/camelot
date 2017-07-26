(ns camelot.model.survey-taxonomy
  (:require
   [camelot.system.state :refer [State]]
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.translation.core :as tr]))

;; NOTE: 030_survey_taxonomy_migration needs to be updated should this model
;; change.

(def query (db/with-db-keys :survey-taxonomy))

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
  (->> (query state :get-all)
       (map survey-taxonomy)))

(s/defn get-all-for-survey :- [SurveyTaxonomy]
  "Retrieve all entries for a given survey."
  [state :- State
   survey-id :- s/Int]
  (->> {:survey-id survey-id}
       (query state :get-all-for-survey)
       (map survey-taxonomy)))

(s/defn get-all-for-taxonomy :- [SurveyTaxonomy]
  "Retrieve all entries for a given taxonomy."
  [state :- State
   taxonomy-id :- s/Int]
  (->> {:taxonomy-id taxonomy-id}
       (query state :get-all-for-taxonomy)
       (map survey-taxonomy)))

(s/defn get-specific :- (s/maybe SurveyTaxonomy)
  "Retrieve the entry with the given ID."
  [state :- State
   id :- s/Int]
  (->> {:survey-taxonomy-id id}
       (query state :get-specific)
       (map survey-taxonomy)
       first))

(s/defn get-specific-by-relations :- (s/maybe SurveyTaxonomy)
  "Retrieve the survey taxonomy with the given survey and taxonomy ID."
  [state :- State
   survey-id :- s/Int
   taxonomy-id :- s/Int]
  (->> {:survey-id survey-id
        :taxonomy-id taxonomy-id}
       (query state :get-specific-by-relations)
       (map survey-taxonomy)
       first))

(s/defn create! :- SurveyTaxonomy
  [state :- State
   data :- TSurveyTaxonomy]
  (let [record (query state :create<! data)]
    (survey-taxonomy (get-specific state (int (:1 record))))))

(s/defn get-or-create! :- SurveyTaxonomy
  [state :- State
   data :- TSurveyTaxonomy]
  (or (get-specific-by-relations state (:survey-id data) (:taxonomy-id data))
      (create! state data)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (query state :delete! {:survey-taxonomy-id id}))
