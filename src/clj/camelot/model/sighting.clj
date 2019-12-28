(ns camelot.model.sighting
  "Sighting models and data access."
  (:require
   [camelot.util.data :as data-util]
   [camelot.spec.schema.state :refer [State]]
   [camelot.model.sighting-field-value :as sighting-field-value]
   [camelot.model.bounding-box :as bounding-box]
   [camelot.model.suggestion :as suggestion]
   [camelot.util.db :as db]
   [schema.core :as s])
  (:import
   (camelot.model.bounding_box BoundingBox)))

(def query (db/with-db-keys :sightings))

(s/defrecord TSighting
    [sighting-quantity :- s/Int
     taxonomy-id :- s/Int
     media-id :- s/Int
     bounding-box-id :- s/Int
     sighting-fields :- (s/maybe {s/Int s/Str})]
  {s/Any s/Any})

(s/defrecord TSightingUpdate
    [sighting-quantity :- s/Int
     taxonomy-id :- s/Int
     sighting-fields :- (s/maybe {s/Int s/Str})]
  {s/Any s/Any})

(s/defrecord Sighting
    [sighting-id :- s/Int
     sighting-created :- org.joda.time.DateTime
     sighting-updated :- org.joda.time.DateTime
     sighting-quantity :- s/Int
     bounding-box :- (s/maybe BoundingBox)
     taxonomy-id :- (s/maybe s/Int)
     media-id :- s/Int
     sighting-label :- s/Str]
  {s/Any s/Any})

(defn sighting
  [data]
  (-> data
      (assoc :sighting-label (str (:sighting-quantity data) "x "
                                  (:taxonomy-genus data) " "
                                  (:taxonomy-species data)))
      (data-util/key-prefix-to-map [:bounding-box])
      (data-util/dissoc-if :bounding-box #(nil? (-> % :bounding-box :id)))
      map->Sighting))

(s/defn tsighting :- TSighting
  [data]
  (map->TSighting data))

(s/defn tsighting-update :- TSightingUpdate
  [data]
  (map->TSightingUpdate data))

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

(defn set-bounding-box!
  "Set the bounding box for `sighting-id`. `bounding-box-id` may be `nil`."
  [state sighting-id bounding-box-id]
  (let [sighting (sighting (get-specific state sighting-id))]
    (db/with-transaction [s state]
      (if-let [bounding-box-id (:bounding-box-id sighting)]
        (bounding-box/delete! s bounding-box-id))
      (query s :set-bounding-box! {:sighting-id sighting-id
                                       :bounding-box-id bounding-box-id}))))

(s/defn create!
  [state data]
  (db/with-transaction [s state]
    (let [record (query s :create<! data)
          sighting-id (int (:1 record))]
      (create-sighting-field-value! s sighting-id (:sighting-fields data))
      (let [sighting (get-specific s sighting-id)]
        (if-let [bounding-box-id (:bounding-box-id sighting)]
          (suggestion/delete-with-bounding-box! s bounding-box-id))
        sighting))))

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
  (let [sighting (get-specific state id)]
    (db/with-transaction [s state]
      (if-let [bounding-box-id (get-in sighting [:bounding-box :id])]
        (bounding-box/delete! s bounding-box-id))
      (sighting-field-value/delete-for-sighting! s id)
      (query s :delete! {:sighting-id id}))))

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
