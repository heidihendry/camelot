(ns camelot.model.species
  "Species models and data access."
  (:require
   [schema.core :as sch]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.db :as db]))

(def query (db/with-db-keys :species))

(sch/defrecord TSpecies
    [species-scientific-name :- sch/Str
     species-common-name :- sch/Str
     species-notes :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(sch/defrecord Species
    [species-id :- sch/Int
     species-created :- org.joda.time.DateTime
     species-updated :- org.joda.time.DateTime
     species-scientific-name :- sch/Str
     species-common-name :- sch/Str
     species-notes :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(def species map->Species)
(def tspecies map->TSpecies)

(sch/defn get-all :- [Species]
  [state :- State]
  (map species (query state :get-all)))

(sch/defn get-specific :- (sch/maybe Species)
  [state :- State
   id :- sch/Int]
  (some->> {:species-id id}
           (query state :get-specific)
           (first)
           (species)))

(sch/defn get-specific-by-scientific-name :- (sch/maybe Species)
  [state :- State
   data :- TSpecies]
  (some->> data
           (query state :get-specific-by-scientific-name)
           (first)
           (species)))

(sch/defn create! :- Species
  [state :- State
   data :- TSpecies]
  (let [record (query state :create<! data)]
    (species (get-specific state (int (:1 record))))))

(sch/defn update! :- Species
  [state :- State
   id :- sch/Int
   data :- TSpecies]
  (query state :update! (merge data {:species-id id}))
  (species (get-specific state id)))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (query state :delete! {:species-id id})
  nil)

(sch/defn get-or-create! :- Species
  [state :- State
   data :- TSpecies]
  (or (get-specific-by-scientific-name state data)
      (create! state data)))
