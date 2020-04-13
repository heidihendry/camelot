(ns camelot.model.associated-taxonomy
  "Model to associate survey with taxonomy details."
  (:require
   [camelot.model.taxonomy :as taxonomy]
   [camelot.spec.schema.state :refer [State]]
   [schema.core :as sch]
   [camelot.util.db :as db]
   [camelot.model.survey :as survey]
   [camelot.model.survey-taxonomy :as survey-taxonomy])
  (:import
   (camelot.model.taxonomy TTaxonomy Taxonomy)))

(sch/defrecord TAssociatedTaxonomy
    [taxonomy-class :- (sch/maybe sch/Str)
     taxonomy-order :- (sch/maybe sch/Str)
     taxonomy-family :- (sch/maybe sch/Str)
     taxonomy-genus :- (sch/maybe sch/Str)
     taxonomy-species :- sch/Str
     taxonomy-common-name :- (sch/maybe sch/Str)
     species-mass-id :- (sch/maybe sch/Int)
     taxonomy-notes :- (sch/maybe sch/Str)
     survey-id :- (sch/maybe sch/Int)]
  {sch/Any sch/Any})

(def tassociated-taxonomy map->TAssociatedTaxonomy)

(sch/defn ensure-associated
  [state :- State
   survey-id :- sch/Int
   taxonomy-id :- sch/Int]
  (or (survey-taxonomy/get-specific-by-relations state survey-id taxonomy-id)
      (let [tst (survey-taxonomy/tsurvey-taxonomy
                 {:survey-id survey-id
                  :taxonomy-id taxonomy-id})]
        (survey-taxonomy/create! state tst))))

(sch/defn create!
  "Associate taxonomy with a survey, or, if not specified, all surveys."
  [state :- State
   taxdata :- TAssociatedTaxonomy]
  (db/with-transaction [s state]
    (let [t (taxonomy/get-or-create! s (taxonomy/ttaxonomy taxdata))
          sid (:survey-id taxdata)]
      (if sid
        (ensure-associated s sid (:taxonomy-id t))
        (doseq [cs (map :survey-id (survey/get-all s))]
          (ensure-associated s cs (:taxonomy-id t))))
      t)))
