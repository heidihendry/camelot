(ns camelot.db.photo
  "Photo models and data access."
  (:require
   [schema.core :as s]
   [camelot.db.core :as db]
   [camelot.app.state :refer [State]]
   [yesql.core :as sql]))

(sql/defqueries "sql/photos.sql")

(s/defrecord TPhoto
    [photo-iso-setting :- (s/maybe s/Int)
     photo-exposure-value :- (s/maybe s/Str)
     photo-flash-setting :- (s/maybe s/Str)
     photo-focal-length :- (s/maybe s/Str)
     photo-fnumber-setting :- (s/maybe s/Str)
     photo-orientation :- (s/maybe s/Str)
     photo-resolution-x :- s/Int
     photo-resolution-y :- s/Int
     media-id :- s/Int])

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
     media-id :- s/Int])

(s/defn tphoto :- TPhoto
  [{:keys [photo-iso-setting photo-exposure-value photo-flash-setting
           photo-focal-length photo-fnumber-setting photo-orientation
           photo-resolution-x photo-resolution-y media-id]}]
  (->TPhoto photo-iso-setting photo-exposure-value photo-flash-setting
            photo-focal-length photo-fnumber-setting photo-orientation
            photo-resolution-x photo-resolution-y media-id))

(s/defn photo :- Photo
  [{:keys [photo-id photo-created photo-updated photo-iso-setting
           photo-exposure-value photo-flash-setting photo-focal-length
           photo-fnumber-setting photo-orientation photo-resolution-x
           photo-resolution-y media-id]}]
  (->Photo photo-id photo-created photo-updated photo-iso-setting
           photo-exposure-value photo-flash-setting photo-focal-length
           photo-fnumber-setting photo-orientation photo-resolution-x
           photo-resolution-y media-id))

(s/defn get-all :- [Photo]
  [state :- State
   id :- s/Num]
  (map photo (db/with-db-keys state -get-all {:media-id id})))

(s/defn get-specific :- Photo
  [state :- State
   id :- s/Num]
  (some->> {:photo-id id}
           (db/with-db-keys state -get-specific)
           (first)
           (photo)))

(s/defn create! :- Photo
  [state :- State
   data :- TPhoto]
  (let [record (db/with-db-keys state -create<! data)]
    (photo (get-specific state (int (:1 record))))))

(s/defn update! :- Photo
  [state :- State
   id :- s/Int
   data :- TPhoto]
  (db/with-db-keys state -update! (merge data {:photo-id id}))
  (photo (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -delete! {:photo-id id})
  nil)
