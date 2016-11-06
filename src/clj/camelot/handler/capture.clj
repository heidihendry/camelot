(ns camelot.handler.capture
  (:require
   [schema.core :as s]
   [camelot.util.capture :as capture]
   [camelot.import.db :as im.db]
   [camelot.model.state :refer [State]]
   [camelot.import.dirtree :as dt]
   [camelot.import.photo :as photo]
   [camelot.db :as db]
   [camelot.handler.import :as import]
   [camelot.model.trap-station-session :as trap-station-session]
   [clj-time.core :as t]
   [camelot.translation.core :as tr])
  (:import (camelot.model.trap_station_session TrapStationSession)))

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
  [state session-camera-id fmt photo filename]
  (db/with-transaction [s state]
    (-create-record! state session-camera-id photo filename fmt)))

(s/defn valid-session-date? :- s/Bool
  [sess :- TrapStationSession
   date :- org.joda.time.DateTime]
  (or (t/after? date (t/plus (:trap-station-session-end-date sess) (t/days 1)))
      (t/before? date (:trap-station-session-start-date sess))))

(defn create-media
  [state content-type tempfile size session-camera-id photo]
  (let [fmt (get capture/image-mimes content-type)
        filename (java.util.UUID/randomUUID)]
    (import/create-image-files tempfile filename fmt)
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
    (if (valid-session-date? sess (:datetime photo))
      {:error (tr/translate (:config state) ::timestamp-outside-range)}
      (create-media state content-type tempfile size session-camera-id photo))))
