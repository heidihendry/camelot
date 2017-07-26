(ns camelot.model.taxonomy
  (:require
   [schema.core :as s]
   [camelot.system.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.util.config :as config]))

(def query (db/with-db-keys :taxonomy))

(s/defrecord TTaxonomy
    [taxonomy-class :- (s/maybe s/Str)
     taxonomy-order :- (s/maybe s/Str)
     taxonomy-family :- (s/maybe s/Str)
     taxonomy-genus :- (s/maybe s/Str)
     taxonomy-species :- s/Str
     taxonomy-common-name :- (s/maybe s/Str)
     species-mass-id :- (s/maybe s/Int)
     taxonomy-notes :- (s/maybe s/Str)]
  {s/Any s/Any})

(s/defrecord Taxonomy
    [taxonomy-id :- s/Int
     taxonomy-created :- org.joda.time.DateTime
     taxonomy-updated :- org.joda.time.DateTime
     taxonomy-class :- (s/maybe s/Str)
     taxonomy-order :- (s/maybe s/Str)
     taxonomy-family :- (s/maybe s/Str)
     taxonomy-genus :- (s/maybe s/Str)
     taxonomy-species :- s/Str
     taxonomy-common-name :- (s/maybe s/Str)
     species-mass-id :- (s/maybe s/Int)
     taxonomy-notes :- (s/maybe s/Str)
     taxonomy-label :- s/Str]
  {s/Any s/Any})

(def taxonomy map->Taxonomy)
(def ttaxonomy map->TTaxonomy)

(defn- add-label
  "Assoc a key for the label, which is a computed value."
  [state rec]
  (assoc rec :taxonomy-label
         (if (= :common-name (config/lookup state :species-name-style))
           (:taxonomy-common-name rec)
           (format "%s %s" (:taxonomy-genus rec) (:taxonomy-species rec)))))

(s/defn get-all :- [Taxonomy]
  [state :- State]
  (map (comp taxonomy (partial add-label state))
       (query state :get-all)))

(s/defn get-all-for-survey :- [Taxonomy]
  [state :- State
   survey-id :- s/Int]
  (some->> {:survey-id survey-id}
           (query state :get-all-for-survey)
           (map (partial add-label state))
           (map taxonomy)))

(s/defn get-specific :- (s/maybe Taxonomy)
  [state :- State
   id :- s/Int]
  (some->> {:taxonomy-id id}
           (query state :get-specific)
           first
           (add-label state)
           taxonomy))

(s/defn get-specific-by-taxonomy :- (s/maybe Taxonomy)
  [state :- State
   data :- TTaxonomy]
  (some->> data
           (query state :get-specific-by-taxonomy)
           first
           (add-label state)
           taxonomy))

(s/defn create! :- Taxonomy
  [state :- State
   data :- TTaxonomy]
  (let [record (query state :create<! data)]
    (taxonomy (add-label state (get-specific state (int (:1 record)))))))

(s/defn clone! :- Taxonomy
  [state :- State
   data :- Taxonomy]
  (let [record (query state :clone<! data)]
    (taxonomy (add-label state (get-specific state (int (:1 record)))))))

(s/defn update! :- Taxonomy
  [state :- State
   id :- s/Int
   data :- TTaxonomy]
  (query state :update! (merge data {:taxonomy-id id}))
  (taxonomy (add-label state (get-specific state id))))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (query state :delete! {:taxonomy-id id})
  nil)

(s/defn delete-from-survey!
  [state :- State
   {:keys [survey-id taxonomy-id]}]
  (query state :delete-from-survey!
    {:survey-id survey-id :taxonomy-id taxonomy-id})
  nil)

(s/defn get-or-create! :- Taxonomy
  [state :- State
   data :- TTaxonomy]
  (or (get-specific-by-taxonomy state data)
      (create! state data)))
