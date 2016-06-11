(ns camelot.model.photo
  (:require [schema.core :as s]
            [camelot.db :as db]
            [camelot.model.state :refer [State]]
            [yesql.core :as sql]))

(sql/defqueries "sql/photos.sql" {:connection db/spec})

(s/defrecord TPhoto
    [photo_iso_setting :- s/Int
     photo_exposure_value :- s/Str
     photo_flash_setting :- s/Str
     photo_focal_length :- s/Str
     photo_fnumber_setting :- s/Str
     photo_orientation :- s/Str
     photo_resolution_x :- s/Int
     photo_resolution_y :- s/Int
     media_id :- s/Int])

(s/defrecord Photo
    [photo-id :- s/Int
     photo-created :- org.joda.time.DateTime
     photo-updated :- org.joda.time.DateTime
     photo_iso_setting :- s/Int
     photo_exposure_value :- s/Str
     photo_flash_setting :- s/Str
     photo_focal_length :- s/Str
     photo_fnumber_setting :- s/Str
     photo_orientation :- s/Str
     photo_resolution_x :- s/Int
     photo_resolution_y :- s/Int
     media_id :- s/Int])

(s/defn tphoto :- TPhoto
  [{:keys [photo_iso_setting photo_exposure_value photo_flash_setting
           photo_focal_length photo_fnumber_setting photo_orientation
           photo_resolution_x photo_resolution_y media_id]}]
  (->TPhoto photo_iso_setting photo_exposure_value photo_flash_setting
            photo_focal_length photo_fnumber_setting photo_orientation
            photo_resolution_x photo_resolution_y media_id))

(s/defn photo :- Photo
  [{:keys [photo-id photo-created photo-updated photo_iso_setting
           photo_exposure_value photo_flash_setting photo_focal_length
           photo_fnumber_setting photo_orientation photo_resolution_x
           photo_resolution_y media_id]}]
  (->Photo photo-id photo-created photo-updated photo_iso_setting
           photo_exposure_value photo_flash_setting photo_focal_length
           photo_fnumber_setting photo_orientation photo_resolution_x
           photo_resolution_y media_id))

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
