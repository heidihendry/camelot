(ns camelot.handler.capture
  (:require [schema.core :as s]
            [camelot.import.db :as im.db]
            [camelot.model.state :refer [State]]
            [camelot.import.dirtree :as dt]
            [camelot.import.photo :as photo]
            [camelot.db :as db]
            [camelot.handler.import :as import]))

(def image-mimes
  {"image/jpeg" "jpg"
   "image/png" "png"})

(s/defn read-photo
  [state tempfile]
  (->> tempfile
       (dt/file-raw-metadata state)
       (photo/parse state)))

(defn- -create-record!
  [state session-camera-id photo filename fmt]
  (let [media (im.db/create-raw-media! state photo filename fmt session-camera-id)]
    (im.db/create-photo! state (:media-id media) (:settings photo))))

(s/defn create-record!
  [state session-camera-id fmt tempfile filename]
  (let [photo (read-photo state tempfile)]
    (db/with-transaction [s state]
      (-create-record! state session-camera-id photo filename fmt))))

(s/defn import-capture!
  [state :- State
   session-camera-id :- s/Int
   {:keys [content-type :- s/Str
           tempfile :- s/Str
           size :- s/Int]}]
  (let [fmt (get image-mimes content-type)
        filename (java.util.UUID/randomUUID)]
    (import/create-image-files tempfile filename fmt)
    (create-record! state session-camera-id fmt tempfile filename)))
