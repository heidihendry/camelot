(ns camelot.model.associated-taxonomy
  "Model to associate survey with taxonomy details."
  (:require
   [camelot.model.taxonomy :as taxonomy]
   [camelot.spec.schema.state :refer [State]]
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.model.survey :as survey]
   [camelot.model.survey-taxonomy :as survey-taxonomy])
  (:import
   (camelot.model.taxonomy TTaxonomy Taxonomy)))

(s/defrecord TAssociatedTaxonomy
    [taxonomy-class :- (s/maybe s/Str)
     taxonomy-order :- (s/maybe s/Str)
     taxonomy-family :- (s/maybe s/Str)
     taxonomy-genus :- (s/maybe s/Str)
     taxonomy-species :- s/Str
     taxonomy-common-name :- (s/maybe s/Str)
     species-mass-id :- (s/maybe s/Int)
     taxonomy-notes :- (s/maybe s/Str)
     survey-id :- (s/maybe s/Int)]
  {s/Any s/Any})

(def tassociated-taxonomy map->TAssociatedTaxonomy)

(s/defn ensure-associated
  [state :- State
   survey-id :- s/Int
   taxonomy-id :- s/Int]
  (or (survey-taxonomy/get-specific-by-relations state survey-id taxonomy-id)
      (let [tst (survey-taxonomy/tsurvey-taxonomy
                 {:survey-id survey-id
                  :taxonomy-id taxonomy-id})]
        (survey-taxonomy/create! state tst))))

(s/defn create!
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
