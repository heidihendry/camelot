(ns camelot.model.species
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]))

(sql/defqueries "sql/species.sql" {:connection db/spec})

(s/defrecord TSpecies
    [species-scientific-name :- s/Str
     species-common-name :- s/Str
     species-notes :- s/Str])

(s/defrecord Species
    [species-id :- s/Int
     species-created :- org.joda.time.DateTime
     species-updated :- org.joda.time.DateTime
     species-scientific-name :- s/Str
     species-common-name :- s/Str
     species-notes :- (s/maybe s/Str)])

(s/defn tspecies :- TSpecies
  [{:keys [species-scientific-name species-common-name species-notes]}]
  (->TSpecies species-scientific-name species-common-name species-notes))

(s/defn species :- Species
  [{:keys [species-id species-created species-updated species-scientific-name
           species-common-name species-notes]}]
  (->Species species-id species-created species-updated species-scientific-name
             species-common-name species-notes))

(s/defn get-all :- [Species]
  [state :- State]
  (map species (db/clj-keys (db/with-connection (:connection state)
                              -get-all))))

(s/defn get-specific :- (s/maybe Species)
  [state :- State
   id :- s/Int]
  (some->> {:species-id id}
           (db/with-db-keys state -get-specific)
           (first)
           (species)))

(s/defn get-specific-by-scientific-name :- (s/maybe Species)
  [state :- State
   data :- TSpecies]
  (some->> data
           (db/with-db-keys state -get-specific-by-scientific-name)
           (first)
           (species)))

(s/defn create! :- Species
  [state :- State
   data :- TSpecies]
  (let [record (db/with-db-keys state -create<! data)]
    (species (get-specific state (int (:1 record))))))

(s/defn update! :- Species
  [state :- State
   id :- s/Int
   data :- TSpecies]
  (db/with-db-keys state -update! (merge data {:species-id id}))
  (species (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -delete! {:species-id id})
  nil)

(s/defn get-or-create! :- Species
  [state :- State
   data :- TSpecies]
  (or (get-specific-by-scientific-name state data)
      (create! state data)))
