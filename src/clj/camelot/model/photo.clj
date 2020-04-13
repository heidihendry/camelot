(ns camelot.model.photo
  "Photo models and data access."
  (:require
   [schema.core :as sch]
   [camelot.util.db :as db]
   [camelot.spec.schema.state :refer [State]]))

(def query (db/with-db-keys :photos))

(sch/defrecord TPhoto
    [photo-iso-setting :- (sch/maybe sch/Int)
     photo-exposure-value :- (sch/maybe sch/Str)
     photo-flash-setting :- (sch/maybe sch/Str)
     photo-focal-length :- (sch/maybe sch/Str)
     photo-fnumber-setting :- (sch/maybe sch/Str)
     photo-orientation :- (sch/maybe sch/Str)
     photo-resolution-x :- sch/Int
     photo-resolution-y :- sch/Int
     media-id :- sch/Int]
  {sch/Any sch/Any})

(sch/defrecord Photo
    [photo-id :- sch/Int
     photo-created :- org.joda.time.DateTime
     photo-updated :- org.joda.time.DateTime
     photo-iso-setting :- (sch/maybe sch/Int)
     photo-exposure-value :- (sch/maybe sch/Str)
     photo-flash-setting :- (sch/maybe sch/Str)
     photo-focal-length :- (sch/maybe sch/Str)
     photo-fnumber-setting :- (sch/maybe sch/Str)
     photo-orientation :- (sch/maybe sch/Str)
     photo-resolution-x :- sch/Int
     photo-resolution-y :- sch/Int
     media-id :- sch/Int]
  {sch/Any sch/Any})

(def photo map->Photo)
(def tphoto map->TPhoto)

(sch/defn get-all :- [Photo]
  [state :- State
   id :- sch/Num]
  (map photo (query state :get-all {:media-id id})))

(sch/defn get-all*
  [state]
  (map photo (query state :get-all* {})))

(sch/defn get-specific :- Photo
  [state :- State
   id :- sch/Num]
  (some->> {:photo-id id}
           (query state :get-specific)
           first
           photo))

(sch/defn create! :- Photo
  [state :- State
   data :- TPhoto]
  (let [record (query state :create<! data)]
    (get-specific state (int (:1 record)))))

(sch/defn update! :- Photo
  [state :- State
   id :- sch/Int
   data :- TPhoto]
  (query state :update! (merge data {:photo-id id}))
  (get-specific state id))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (query state :delete! {:photo-id id})
  nil)
