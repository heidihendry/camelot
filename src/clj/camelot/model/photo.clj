(ns camelot.model.photo
  "Photo models and data access."
  (:require
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.spec.schema.state :refer [State]]))

(def query (db/with-db-keys :photos))

(s/defrecord TPhoto
    [photo-iso-setting :- (s/maybe s/Int)
     photo-exposure-value :- (s/maybe s/Str)
     photo-flash-setting :- (s/maybe s/Str)
     photo-focal-length :- (s/maybe s/Str)
     photo-fnumber-setting :- (s/maybe s/Str)
     photo-orientation :- (s/maybe s/Str)
     photo-resolution-x :- s/Int
     photo-resolution-y :- s/Int
     media-id :- s/Int]
  {s/Any s/Any})

(s/defrecord Photo
    [photo-id :- s/Int
     photo-created :- org.joda.time.DateTime
     photo-updated :- org.joda.time.DateTime
     photo-iso-setting :- (s/maybe s/Int)
     photo-exposure-value :- (s/maybe s/Str)
     photo-flash-setting :- (s/maybe s/Str)
     photo-focal-length :- (s/maybe s/Str)
     photo-fnumber-setting :- (s/maybe s/Str)
     photo-orientation :- (s/maybe s/Str)
     photo-resolution-x :- s/Int
     photo-resolution-y :- s/Int
     media-id :- s/Int]
  {s/Any s/Any})

(def photo map->Photo)
(def tphoto map->TPhoto)

(s/defn get-all :- [Photo]
  [state :- State
   id :- s/Num]
  (map photo (query state :get-all {:media-id id})))

(s/defn get-all* :- [Photo]
  [state :- State]
  (map photo (query state :get-all* {})))

(s/defn get-specific :- Photo
  [state :- State
   id :- s/Num]
  (some->> {:photo-id id}
           (query state :get-specific)
           first
           photo))

(s/defn create! :- Photo
  [state :- State
   data :- TPhoto]
  (let [record (query state :create<! data)]
    (get-specific state (int (:1 record)))))

(s/defn update! :- Photo
  [state :- State
   id :- s/Int
   data :- TPhoto]
  (query state :update! (merge data {:photo-id id}))
  (get-specific state id))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (query state :delete! {:photo-id id})
  nil)
