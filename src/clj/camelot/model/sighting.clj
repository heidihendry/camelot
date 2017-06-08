(ns camelot.model.sighting
  "Sighting models and data access."
  (:require
   [yesql.core :as sql]
   [camelot.system.state :refer [State]]
   [camelot.model.sighting-field-value :as sighting-field-value]
   [camelot.util.db :as db]
   [schema.core :as s]))

(sql/defqueries "sql/sightings.sql")

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
  [{:keys [sighting-id sighting-created sighting-updated sighting-quantity
           sighting-lifestage sighting-sex taxonomy-id media-id taxonomy-genus
           taxonomy-species]}]
  (->Sighting sighting-id sighting-created sighting-updated sighting-quantity
              (or sighting-lifestage sighting-default-option)
              (or sighting-sex sighting-default-option)
              taxonomy-id media-id
              (str sighting-quantity "x " taxonomy-genus " " taxonomy-species)))

(s/defn tsighting :- TSighting
  [{:keys [sighting-quantity sighting-lifestage sighting-sex taxonomy-id
           media-id sighting-fields]}]
  (->TSighting sighting-quantity (known-or-nil sighting-lifestage)
               (known-or-nil sighting-sex) taxonomy-id media-id
               sighting-fields))

(s/defn get-all
  [state :- State
   id :- s/Num]
  (map sighting (db/with-db-keys state -get-all {:media-id id})))

(s/defn get-all*
  [state :- State]
  (map sighting (db/clj-keys (db/with-connection state -get-all*))))

(s/defn get-specific
  [state :- State
   id :- s/Int]
  (some->> {:sighting-id id}
           (db/with-db-keys state -get-specific)
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
  (clojure.tools.logging/error data)
  (let [record (db/with-db-keys state -create<! data)
        sighting-id (int (:1 record))]
    (create-sighting-field-value! state sighting-id (:sighting-fields data))
    (sighting (get-specific state sighting-id))))

(s/defn update!
  [state :- State
   id :- s/Int
   data]
  (db/with-db-keys state -update! (merge data {:sighting-id id}))
  (sighting (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-transaction [s state]
    (sighting-field-value/delete-for-sighting! state id)
    (db/with-db-keys state -delete! {:sighting-id id})))

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
  (db/with-db-keys state -get-available {:sighting-id id}))

(s/defn get-alternatives
  [state :- State
   id :- s/Int]
  (let [res (get-specific state id)]
    (db/with-db-keys state -get-alternatives res)))
