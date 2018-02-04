(ns camelot.model.species
  "Species models and data access."
  (:require
   [schema.core :as s]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.db :as db]))

(def query (db/with-db-keys :species))

(s/defrecord TSpecies
    [species-scientific-name :- s/Str
     species-common-name :- s/Str
     species-notes :- (s/maybe s/Str)]
  {s/Any s/Any})

(s/defrecord Species
    [species-id :- s/Int
     species-created :- org.joda.time.DateTime
     species-updated :- org.joda.time.DateTime
     species-scientific-name :- s/Str
     species-common-name :- s/Str
     species-notes :- (s/maybe s/Str)]
  {s/Any s/Any})

(def species map->Species)
(def tspecies map->TSpecies)

(s/defn get-all :- [Species]
  [state :- State]
  (map species (query state :get-all)))

(s/defn get-specific :- (s/maybe Species)
  [state :- State
   id :- s/Int]
  (some->> {:species-id id}
           (query state :get-specific)
           (first)
           (species)))

(s/defn get-specific-by-scientific-name :- (s/maybe Species)
  [state :- State
   data :- TSpecies]
  (some->> data
           (query state :get-specific-by-scientific-name)
           (first)
           (species)))

(s/defn create! :- Species
  [state :- State
   data :- TSpecies]
  (let [record (query state :create<! data)]
    (species (get-specific state (int (:1 record))))))

(s/defn update! :- Species
  [state :- State
   id :- s/Int
   data :- TSpecies]
  (query state :update! (merge data {:species-id id}))
  (species (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (query state :delete! {:species-id id})
  nil)

(s/defn get-or-create! :- Species
  [state :- State
   data :- TSpecies]
  (or (get-specific-by-scientific-name state data)
      (create! state data)))
