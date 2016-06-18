(ns camelot.model.taxonomy
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]))

(sql/defqueries "sql/taxonomy.sql" {:connection db/spec})

(s/defrecord TTaxonomy
    [taxonomy-class :- (s/maybe s/Str)
     taxonomy-order :- (s/maybe s/Str)
     taxonomy-family :- (s/maybe s/Str)
     taxonomy-genus :- (s/maybe s/Str)
     taxonomy-species :- s/Str
     taxonomy-common-name :- s/Str
     taxonomy-notes :- (s/maybe s/Str)])

(s/defrecord Taxonomy
    [taxonomy-id :- s/Int
     taxonomy-created :- org.joda.time.DateTime
     taxonomy-updated :- org.joda.time.DateTime
     taxonomy-class :- (s/maybe s/Str)
     taxonomy-order :- (s/maybe s/Str)
     taxonomy-family :- (s/maybe s/Str)
     taxonomy-genus :- (s/maybe s/Str)
     taxonomy-species :- s/Str
     taxonomy-common-name :- s/Str
     taxonomy-notes :- (s/maybe s/Str)
     taxonomy-label :- s/Str])

(s/defn ttaxonomy :- TTaxonomy
  [{:keys [taxonomy-class taxonomy-order taxonomy-family taxonomy-genus
           taxonomy-species taxonomy-common-name taxonomy-notes]}]
  (->TTaxonomy taxonomy-class taxonomy-order taxonomy-family taxonomy-genus
               taxonomy-species taxonomy-common-name taxonomy-notes))

(s/defn taxonomy :- Taxonomy
  [{:keys [taxonomy-id taxonomy-created taxonomy-updated taxonomy-class
           taxonomy-order taxonomy-family taxonomy-genus taxonomy-species
           taxonomy-common-name taxonomy-notes taxonomy-label]}]
  (->Taxonomy taxonomy-id taxonomy-created taxonomy-updated taxonomy-class
               taxonomy-order taxonomy-family taxonomy-genus taxonomy-species
               taxonomy-common-name taxonomy-notes taxonomy-label))

(defn- add-label
  "Assoc a key for the label, which is a computed value."
  [rec]
  (assoc rec :taxonomy-label
         (format "%s %s" (:taxonomy-genus rec) (:taxonomy-species rec))))

(s/defn get-all :- [Taxonomy]
  [state :- State]
  (map (comp taxonomy add-label)
       (db/clj-keys (db/with-connection (:connection state)
                      -get-all))))

(s/defn get-specific :- (s/maybe Taxonomy)
  [state :- State
   id :- s/Int]
  (some->> {:taxonomy-id id}
           (db/with-db-keys state -get-specific)
           first
           add-label
           taxonomy))

(s/defn get-specific-by-taxonomy :- (s/maybe Taxonomy)
  [state :- State
   data :- TTaxonomy]
  (some->> data
           (db/with-db-keys state -get-specific-by-taxonomy)
           first
           add-label
           taxonomy))

(s/defn create! :- Taxonomy
  [state :- State
   data :- TTaxonomy]
  (let [record (db/with-db-keys state -create<! data)]
    (taxonomy (add-label (get-specific state (int (:1 record)))))))

(s/defn clone! :- Taxonomy
  [state :- State
   data :- Taxonomy]
  (let [record (db/with-db-keys state -clone<! data)]
    (taxonomy (add-label (get-specific state (int (:1 record)))))))

(s/defn update! :- Taxonomy
  [state :- State
   id :- s/Int
   data :- TTaxonomy]
  (db/with-db-keys state -update! (merge data {:taxonomy-id id}))
  (taxonomy (add-label (get-specific state id))))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -delete! {:taxonomy-id id})
  nil)

(s/defn get-or-create! :- Taxonomy
  [state :- State
   data :- TTaxonomy]
  (or (get-specific-by-taxonomy state data)
      (create! state data)))
