(ns camelot.model.sighting
  (:require [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]
            [schema.core :as s]))

(sql/defqueries "sql/sightings.sql" {:connection db/spec})

(s/defrecord TSighting
    [sighting-quantity :- s/Int
     species-id :- s/Int
     media-id :- s/Int])

(s/defrecord Sighting
    [sighting-id :- s/Int
     sighting-created :- org.joda.time.DateTime
     sighting-updated :- org.joda.time.DateTime
     sighting-quantity :- s/Int
     species-id :- s/Int
     media-id :- s/Int])

(s/defn sighting :- Sighting
  [{:keys [sighting-id sighting-created sighting-updated sighting-quantity
           species-id media-id]}]
  (->Sighting sighting-id sighting-created sighting-updated sighting-quantity
              species-id media-id))

(s/defn tsighting :- TSighting
  [{:keys [sighting-quantity species-id media-id]}]
  (->TSighting sighting-quantity species-id media-id))

(s/defn get-all
  [state :- State
   id :- s/Num]
  (map sighting (db/with-db-keys state -get-all {:media-id id})))

(s/defn get-all*
  [state :- State]
  (map sighting (db/with-db-keys state -get-all* {})))

(s/defn get-specific
  [state :- State
   id :- s/Int]
  (some->> {:sighting-id id}
           (db/with-db-keys state -get-specific)
           (first)
           (sighting)))

(s/defn create!
  [state :- State
   data :- TSighting]
  (let [record (db/with-db-keys state -create<! data)]
    (sighting (get-specific state (int (:1 record))))))

(s/defn update!
  [state :- State
   id :- s/Int
   data]
  (db/with-db-keys state -update! (merge data {:sighting-id id}))
  (sighting (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -delete! {:sighting-id id}))

(s/defn get-available
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -get-available {:sighting-id id}))

(s/defn get-alternatives
  [state :- State
   id :- s/Int]
  (let [res (get-specific state id)]
    (db/with-db-keys state -get-alternatives res)))
