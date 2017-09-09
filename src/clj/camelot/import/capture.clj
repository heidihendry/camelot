(ns camelot.import.capture
  "Camera check capture import."
  (:require
   [schema.core :as s]
   [camelot.util.capture :as capture]
   [camelot.import.video :as video]
   [camelot.system.state :refer [State]]
   [camelot.import.dirtree :as dt]
   [camelot.import.metadata-utils :as mutil]
   [camelot.util.db :as db]
   [camelot.import.image :as image]
   [camelot.import.cameras :as import.cameras]
   [camelot.model.media :as media]
   [camelot.model.photo :as photo]
   [camelot.model.trap-station-session :as trap-station-session]
   [clj-time.core :as t]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [camelot.translation.core :as tr])
  (:import
   (camelot.model.trap_station_session TrapStationSession)))

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

(s/defn read-photo
  "Read and standardise file metadata from an image."
  [state tempfile]
  (mutil/parse state (dt/file-raw-metadata state tempfile)))

(s/defn read-video
  "Read and standardise file metadata from a video."
  [state session-camera-id file]
  (let [camera (:cuddeback-default import.cameras/cameras)]
    {:datetime (video/get-timestamp camera file)}))

(defn extract-metadata
  "Read and standardise file metadata from a capture."
  [state session-camera-id tempfile]
  (let [f (io/file tempfile)]
    (if (image/video-file? tempfile)
      (read-video state session-camera-id tempfile)
      (read-photo state tempfile))))

(defn create-record!
  "Create the media and photo records."
  [state session-camera-id fmt photo filename]
  (db/with-transaction [s state]
    (let [media (create-media! state photo filename fmt session-camera-id)]
      (->> (:media-id media)
           (assoc photo :media-id)
           photo/tphoto
           (photo/create! state)))))

(s/defn valid-session-date? :- s/Bool
  "Predicate returning true if given date lies between session start and end dates. False otherwise.

24-hour tolerence on session end date applies."
  [sess :- TrapStationSession
   date :- (s/maybe org.joda.time.DateTime)]
  (not (or (nil? date)
           (t/before? date (:trap-station-session-start-date sess))
           (t/after? date (t/plus (:trap-station-session-end-date sess)
                                  (t/days 1))))))

(defn create-media-and-image!
  "Create a set of images and DB records for the input."
  [state content-type tempfile size session-camera-id photo]
  (let [fmt (get capture/image-mimes content-type)
        filename (image/create-image-files state tempfile fmt)]
    (create-record! state session-camera-id fmt photo filename)))

(s/defn import-capture!
  [state :- State
   session-camera-id :- s/Int
   {:keys [content-type :- s/Str
           tempfile :- s/Str
           size :- s/Int]}]
  (let [sess (trap-station-session/get-specific-by-trap-station-session-camera-id
              state session-camera-id)
        capture (extract-metadata state session-camera-id tempfile)]
    (if (or (nil? capture) (not (valid-session-date? sess (:datetime capture))))
      {:error (tr/translate state ::timestamp-outside-range)}
      (create-media-and-image! state content-type tempfile size session-camera-id capture))))
