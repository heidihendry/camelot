(ns camelot.model.taxonomy
  (:require
   [schema.core :as sch]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.util.state :as state]))

(def query (db/with-db-keys :taxonomy))

(sch/defrecord TTaxonomy
    [taxonomy-class :- (sch/maybe sch/Str)
     taxonomy-order :- (sch/maybe sch/Str)
     taxonomy-family :- (sch/maybe sch/Str)
     taxonomy-genus :- (sch/maybe sch/Str)
     taxonomy-species :- sch/Str
     taxonomy-common-name :- (sch/maybe sch/Str)
     species-mass-id :- (sch/maybe sch/Int)
     taxonomy-notes :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(sch/defrecord Taxonomy
    [taxonomy-id :- sch/Int
     taxonomy-created :- org.joda.time.DateTime
     taxonomy-updated :- org.joda.time.DateTime
     taxonomy-class :- (sch/maybe sch/Str)
     taxonomy-order :- (sch/maybe sch/Str)
     taxonomy-family :- (sch/maybe sch/Str)
     taxonomy-genus :- (sch/maybe sch/Str)
     taxonomy-species :- sch/Str
     taxonomy-common-name :- (sch/maybe sch/Str)
     species-mass-id :- (sch/maybe sch/Int)
     taxonomy-notes :- (sch/maybe sch/Str)
     taxonomy-label :- sch/Str]
  {sch/Any sch/Any})

(def taxonomy map->Taxonomy)
(def ttaxonomy map->TTaxonomy)

(defn- add-label
  "Assoc a key for the label, which is a computed value."
  [state rec]
  (assoc rec :taxonomy-label
         (if (= :common-name (state/lookup state :species-name-style))
           (:taxonomy-common-name rec)
           (format "%s %s" (:taxonomy-genus rec) (:taxonomy-species rec)))))

(sch/defn get-all :- [Taxonomy]
  [state :- State]
  (map (comp taxonomy (partial add-label state))
       (query state :get-all)))

(sch/defn get-all-for-survey :- [Taxonomy]
  [state :- State
   survey-id :- sch/Int]
  (some->> {:survey-id survey-id}
           (query state :get-all-for-survey)
           (map (partial add-label state))
           (map taxonomy)))

(sch/defn get-specific :- (sch/maybe Taxonomy)
  [state :- State
   id :- sch/Int]
  (some->> {:taxonomy-id id}
           (query state :get-specific)
           first
           (add-label state)
           taxonomy))

(sch/defn get-specific-by-taxonomy :- (sch/maybe Taxonomy)
  [state :- State
   data :- TTaxonomy]
  (some->> data
           (query state :get-specific-by-taxonomy)
           first
           (add-label state)
           taxonomy))

(sch/defn create! :- Taxonomy
  [state :- State
   data :- TTaxonomy]
  (let [record (query state :create<! data)]
    (taxonomy (add-label state (get-specific state (int (:1 record)))))))

(sch/defn clone! :- Taxonomy
  [state :- State
   data :- Taxonomy]
  (let [record (query state :clone<! data)]
    (taxonomy (add-label state (get-specific state (int (:1 record)))))))

(sch/defn update! :- Taxonomy
  [state :- State
   id :- sch/Int
   data :- TTaxonomy]
  (query state :update! (merge data {:taxonomy-id id}))
  (taxonomy (add-label state (get-specific state id))))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (query state :delete! {:taxonomy-id id})
  nil)

(sch/defn delete-from-survey!
  [state :- State
   {:keys [survey-id taxonomy-id]}]
  (query state :delete-from-survey!
    {:survey-id survey-id :taxonomy-id taxonomy-id})
  nil)

(sch/defn get-or-create! :- Taxonomy
  [state :- State
   data :- TTaxonomy]
  (or (get-specific-by-taxonomy state data)
      (create! state data)))
