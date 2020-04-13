(ns camelot.model.survey-taxonomy
  (:require
   [camelot.spec.schema.state :refer [State]]
   [schema.core :as sch]
   [camelot.util.db :as db]))

;; NOTE: 030_survey_taxonomy_migration needs to be updated should this model
;; change.

(def query (db/with-db-keys :survey-taxonomy))

(sch/defrecord TSurveyTaxonomy
    [survey-id :- sch/Int
     taxonomy-id :- sch/Int]
  {sch/Any sch/Any})

(sch/defrecord SurveyTaxonomy
    [survey-taxonomy-id :-  sch/Int
     survey-taxonomy-created :- org.joda.time.DateTime
     survey-taxonomy-updated :- org.joda.time.DateTime
     survey-id :- sch/Int
     taxonomy-id :- sch/Int]
  {sch/Any sch/Any})

(def survey-taxonomy map->SurveyTaxonomy)
(def tsurvey-taxonomy map->TSurveyTaxonomy)

(sch/defn get-all :- [SurveyTaxonomy]
  "Retrieve all available survey taxonomies."
  [state :- State]
  (->> (query state :get-all)
       (map survey-taxonomy)))

(sch/defn get-all-for-survey :- [SurveyTaxonomy]
  "Retrieve all entries for a given survey."
  [state :- State
   survey-id :- sch/Int]
  (->> {:survey-id survey-id}
       (query state :get-all-for-survey)
       (map survey-taxonomy)))

(sch/defn get-all-for-taxonomy :- [SurveyTaxonomy]
  "Retrieve all entries for a given taxonomy."
  [state :- State
   taxonomy-id :- sch/Int]
  (->> {:taxonomy-id taxonomy-id}
       (query state :get-all-for-taxonomy)
       (map survey-taxonomy)))

(sch/defn get-specific :- (sch/maybe SurveyTaxonomy)
  "Retrieve the entry with the given ID."
  [state :- State
   id :- sch/Int]
  (->> {:survey-taxonomy-id id}
       (query state :get-specific)
       (map survey-taxonomy)
       first))

(sch/defn get-specific-by-relations :- (sch/maybe SurveyTaxonomy)
  "Retrieve the survey taxonomy with the given survey and taxonomy ID."
  [state :- State
   survey-id :- sch/Int
   taxonomy-id :- sch/Int]
  (->> {:survey-id survey-id
        :taxonomy-id taxonomy-id}
       (query state :get-specific-by-relations)
       (map survey-taxonomy)
       first))

(sch/defn create! :- SurveyTaxonomy
  [state :- State
   data :- TSurveyTaxonomy]
  (let [record (query state :create<! data)]
    (survey-taxonomy (get-specific state (int (:1 record))))))

(sch/defn get-or-create! :- SurveyTaxonomy
  [state :- State
   data :- TSurveyTaxonomy]
  (or (get-specific-by-relations state (:survey-id data) (:taxonomy-id data))
      (create! state data)))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (query state :delete! {:survey-taxonomy-id id}))
