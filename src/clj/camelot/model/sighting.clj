(ns camelot.model.sighting
  "Sighting models and data access."
  (:require
   [camelot.util.data :as data-util]
   [camelot.spec.schema.state :refer [State]]
   [camelot.model.sighting-field-value :as sighting-field-value]
   [camelot.model.bounding-box :as bounding-box]
   [camelot.model.suggestion :as suggestion]
   [camelot.util.db :as db]
   [schema.core :as sch])
  (:import
   (camelot.model.bounding_box BoundingBox)))

(def query (db/with-db-keys :sightings))

(sch/defrecord TSighting
    [sighting-quantity :- sch/Int
     taxonomy-id :- sch/Int
     media-id :- sch/Int
     bounding-box-id :- (sch/maybe sch/Int)
     sighting-fields :- (sch/maybe {sch/Int sch/Str})]
  {sch/Any sch/Any})

(sch/defrecord TSightingUpdate
    [sighting-quantity :- sch/Int
     taxonomy-id :- sch/Int
     sighting-fields :- (sch/maybe {sch/Int sch/Str})]
  {sch/Any sch/Any})

(sch/defrecord Sighting
    [sighting-id :- sch/Int
     sighting-created :- org.joda.time.DateTime
     sighting-updated :- org.joda.time.DateTime
     sighting-quantity :- sch/Int
     bounding-box :- (sch/maybe BoundingBox)
     taxonomy-id :- (sch/maybe sch/Int)
     media-id :- sch/Int
     sighting-label :- sch/Str]
  {sch/Any sch/Any})

(defn sighting
  [data]
  (-> data
      (assoc :sighting-label (str (:sighting-quantity data) "x "
                                  (:taxonomy-genus data) " "
                                  (:taxonomy-species data)))
      (data-util/key-prefix-to-map [:bounding-box])
      (data-util/dissoc-if :bounding-box #(nil? (-> % :bounding-box :id)))
      map->Sighting))

(sch/defn tsighting :- TSighting
  [data]
  (map->TSighting data))

(sch/defn tsighting-update :- TSightingUpdate
  [data]
  (map->TSightingUpdate data))

(sch/defn get-all
  [state :- State
   id :- sch/Num]
  (map sighting (query state :get-all {:media-id id})))

(sch/defn get-all*
  [state :- State]
  (let [sf (sighting-field-value/query-all state)]
    (->> (query state :get-all*)
         (map #(sighting (merge (get sf (:sighting-id %)) %))))))

(defn get-all-for-media-ids
  [state media-ids]
  (let [sightings (query state :get-all-for-media-ids {:media-ids media-ids})
        sf (sighting-field-value/get-all-by-sighting-ids state (map :sighting-id sightings))]
    (map #(sighting (merge (get sf (:sighting-id %)) %)) sightings)))

(sch/defn get-specific
  [state :- State
   id :- sch/Int]
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

(sch/defn create!
  [state data]
  (db/with-transaction [s state]
    (let [record (query s :create<! data)
          sighting-id (int (:1 record))]
      (create-sighting-field-value! s sighting-id (:sighting-fields data))
      (let [sighting (get-specific s sighting-id)]
        (when-let [bounding-box-id (:bounding-box-id sighting)]
          (suggestion/delete-with-bounding-box! s bounding-box-id))
        sighting))))

(sch/defn update!
  [state :- State
   id :- sch/Int
   data :- TSightingUpdate]
  (db/with-transaction [s state]
    (query s :update! (merge data {:sighting-id id}))
    (sighting-field-value/update-for-sighting! s id (:sighting-fields data))
    (sighting (get-specific s id))))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (db/with-transaction [s state]
    (sighting-field-value/delete-for-sighting! s id)
    (query s :delete! {:sighting-id id})))

(sch/defn delete-with-media-ids!
  [state :- State
   media-ids]
  (->> media-ids
       (mapcat (partial get-all state))
       (map :sighting-id)
       (map (partial delete! state))))

(sch/defn get-available
  [state :- State
   id :- sch/Int]
  (query state :get-available {:sighting-id id}))

(sch/defn get-alternatives
  [state :- State
   id :- sch/Int]
  (let [res (get-specific state id)]
    (query state :get-alternatives res)))
