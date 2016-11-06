(ns camelot.handler.associated-taxonomy
  (:require
   [camelot.model.taxonomy :as taxonomy]
   [camelot.model.state :refer [State]]
   [schema.core :as s]
   [camelot.db :as db]
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
     survey-id :- (s/maybe s/Int)])

(s/defn tassociated-taxonomy :- TAssociatedTaxonomy
  [{:keys [taxonomy-class taxonomy-order taxonomy-family taxonomy-genus
           taxonomy-species taxonomy-common-name species-mass-id taxonomy-notes survey-id]}]
  (->TAssociatedTaxonomy taxonomy-class taxonomy-order taxonomy-family taxonomy-genus
                         taxonomy-species taxonomy-common-name species-mass-id
                         taxonomy-notes survey-id))

(s/defn get-or-create-taxonomy :- Taxonomy
  [state :- State
   taxdata :- TTaxonomy]
  (or (taxonomy/get-specific-by-taxonomy state taxdata)
      (taxonomy/create! state taxdata)))

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
  [state :- State
   taxdata :- TAssociatedTaxonomy]
  (db/with-transaction [s state]
    (let [t (get-or-create-taxonomy s (taxonomy/ttaxonomy taxdata))
          sid (:survey-id taxdata)]
      (if sid
        (ensure-associated s sid (:taxonomy-id t))
        (doseq [cs (map :survey-id (survey/get-all s))]
          (ensure-associated s cs (:taxonomy-id t))))
      t)))
