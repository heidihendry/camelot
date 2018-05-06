(ns camelot.import.capture
  "Camera check capture import."
  (:require
   [camelot.import.dirtree :as dt]
   [camelot.import.image :as image]
   [camelot.import.metadata-utils :as mutil]
   [camelot.model.media :as media]
   [camelot.model.photo :as photo]
   [camelot.util.capture :as capture]
   [camelot.util.db :as db]
   [clj-time.core :as t]
   [clojure.string :as str]))

(defn create-media!
  "Create media record."
  [state photo filename fmt trap-camera-id]
  (media/create!
   state
   (media/tmedia
    {:media-capture-timestamp (:datetime photo)
     :media-filename (str/lower-case filename)
     :media-format (str/lower-case fmt)
     :media-cameracheck false
     :media-attention-needed false
     :media-processed false
     :media-reference-quality false
     :media-notes nil
     :trap-station-session-camera-id trap-camera-id})))

(defn read-photo
  "Read and standardise file metadata."
  [state tempfile]
  (mutil/parse state (dt/file-raw-metadata state tempfile)))

(defn create-record!
  "Create the media and photo records."
  [state session-camera-id fmt photo filename]
  (db/with-transaction [s state]
    (let [media (create-media! state photo filename fmt session-camera-id)]
      (->> (:media-id media)
           (assoc photo :media-id)
           photo/tphoto
           (photo/create! state)))))

(defn create-media-and-image!
  "Create a set of images and DB records for the input."
  [state content-type tempfile size session-camera-id photo]
  (let [fmt (get capture/image-mimes content-type)
        filename (image/create-image-files state tempfile fmt)]
    (create-record! state session-camera-id fmt photo filename)))
