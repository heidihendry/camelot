(ns camelot.model.sighting
  "Sighting models and data access."
  (:require
   [camelot.system.state :refer [State]]
   [camelot.model.sighting-field-value :as sighting-field-value]
   [camelot.util.db :as db]
   [schema.core :as s]))

(def query (db/with-db-keys :sightings))

(def sighting-default-option "unidentified")

(defn known-or-nil
  [v]
  (when-not (= v sighting-default-option)
    v))

(s/defrecord TSighting
    [sighting-quantity :- s/Int
     sighting-lifestage :- (s/maybe s/Str)
     sighting-sex :- (s/maybe s/Str)
     taxonomy-id :- s/Int
     media-id :- s/Int
     sighting-fields :- (s/maybe {s/Int s/Str})]
  {s/Any s/Any})

(s/defrecord TSightingUpdate
    [sighting-quantity :- s/Int
     sighting-lifestage :- (s/maybe s/Str)
     sighting-sex :- (s/maybe s/Str)
     taxonomy-id :- s/Int
     sighting-fields :- (s/maybe {s/Int s/Str})]
  {s/Any s/Any})

(s/defrecord Sighting
    [sighting-id :- s/Int
     sighting-created :- org.joda.time.DateTime
     sighting-updated :- org.joda.time.DateTime
     sighting-quantity :- s/Int
     sighting-lifestage :- (s/maybe s/Str)
     sighting-sex :- (s/maybe s/Str)
     taxonomy-id :- (s/maybe s/Int)
     media-id :- s/Int
     sighting-label :- s/Str]
  {s/Any s/Any})

(s/defn sighting :- Sighting
  [data]
  (map->Sighting (-> data
                     (update :sighting-lifestage #(or % sighting-default-option))
                     (update :sighting-sex #(or % sighting-default-option))
                     (assoc :sighting-label (str (:sighting-quantity data) "x "
                                                 (:taxonomy-genus data) " "
                                                 (:taxonomy-species data))))))

(s/defn tsighting :- TSighting
  [data]
  (map->TSighting (-> data
                      (update :sighting-lifestage known-or-nil)
                      (update :sighting-sex known-or-nil))))

(s/defn tsighting-update :- TSightingUpdate
  [data]
  (map->TSightingUpdate (-> data
                            (update :sighting-lifestage known-or-nil)
                            (update :sighting-sex known-or-nil))))

(s/defn get-all
  [state :- State
   id :- s/Num]
  (map sighting (query state :get-all {:media-id id})))

(s/defn get-all*
  [state :- State]
  (let [sf (sighting-field-value/query-all state)]
    (->> (query state :get-all*)
                   (map #(sighting (merge (get sf (:sighting-id %)) %))))))

(s/defn get-specific
  [state :- State
   id :- s/Int]
  (some->> {:sighting-id id}
           (query state :get-specific)
           (first)
           (sighting)))

(defn- create-sighting-field-value!
  [state sighting-id field-data]
  (dorun
   (some->> field-data
            seq
            (map (fn [[k v]]
                   (sighting-field-value/create! state sighting-id k v))))))

(s/defn create!
  [state :- State
   data :- TSighting]
  (let [record (query state :create<! data)
        sighting-id (int (:1 record))]
    (create-sighting-field-value! state sighting-id (:sighting-fields data))
    (sighting (get-specific state sighting-id))))

(s/defn update!
  [state :- State
   id :- s/Int
   data :- TSightingUpdate]
  (db/with-transaction [s state]
    (query s :update! (merge data {:sighting-id id}))
    (sighting-field-value/update-for-sighting! s id (:sighting-fields data))
    (sighting (get-specific s id))))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-transaction [s state]
    (sighting-field-value/delete-for-sighting! state id)
    (query state :delete! {:sighting-id id})))

(s/defn delete-with-media-ids!
  [state :- State
   media-ids]
  (->> media-ids
       (mapcat (partial get-all state))
       (map :sighting-id)
       (map (partial delete! state))))

(s/defn get-available
  [state :- State
   id :- s/Int]
  (query state :get-available {:sighting-id id}))

(s/defn get-alternatives
  [state :- State
   id :- s/Int]
  (let [res (get-specific state id)]
    (query state :get-alternatives res)))
