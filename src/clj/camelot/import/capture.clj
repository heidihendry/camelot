(ns camelot.import.capture
  (:require
   [schema.core :as s]
   [camelot.util.capture :as capture]
   [camelot.system.state :refer [State]]
   [camelot.import.dirtree :as dt]
   [camelot.import.metadata-utils :as mutil]
   [camelot.util.db :as db]
   [camelot.import.core :as import]
   [camelot.model.media :as media]
   [camelot.model.photo :as photo]
   [camelot.model.trap-station-session :as trap-station-session]
   [clj-time.core :as t]
   [clojure.string :as str]
   [camelot.translation.core :as tr])
  (:import
   (camelot.model.trap_station_session TrapStationSession)))

(defn create-media!
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
  [state tempfile]
  (->> tempfile
       (dt/file-raw-metadata state)
       (mutil/parse state)))

(defn- -create-record!
  [state session-camera-id photo filename fmt]
  (let [media (create-media! state photo filename fmt session-camera-id)]
    (photo/create! state
                   (photo/tphoto (assoc photo :media-id (:media-id media))))))

(s/defn create-record!
  [state session-camera-id fmt photo filename]
  (db/with-transaction [s state]
    (-create-record! state session-camera-id photo filename fmt)))

(s/defn invalid-session-date? :- s/Bool
  [sess :- TrapStationSession
   date :- org.joda.time.DateTime]
  (or (nil? date)
      (t/after? date (t/plus (:trap-station-session-end-date sess) (t/days 1)))
      (t/before? date (:trap-station-session-start-date sess))))

(defn create-media
  [state content-type tempfile size session-camera-id photo]
  (let [fmt (get capture/image-mimes content-type)
        filename (import/create-image-files state tempfile fmt)]
    (create-record! state session-camera-id fmt photo filename)))

(s/defn import-capture!
  [state :- State
   session-camera-id :- s/Int
   {:keys [content-type :- s/Str
           tempfile :- s/Str
           size :- s/Int]}]
  (let [sess (trap-station-session/get-specific-by-trap-station-session-camera-id
              state session-camera-id)
        photo (read-photo state tempfile)]
    (if (or (nil? photo)
            (invalid-session-date? sess (:datetime photo)))
      {:error (tr/translate state ::timestamp-outside-range)}
      (create-media state content-type tempfile size session-camera-id photo))))
