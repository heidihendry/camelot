(ns camelot.model.media
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]
            [clojure.java.io :as io]
            [camelot.util.java-file :as jf]
            [camelot.util.config :as config]
            [clojure.string :as str])
  (:import [org.apache.commons.lang3 SystemUtils]))

(sql/defqueries "sql/media.sql" {:connection db/spec})

(s/defrecord TMedia
    [media-filename :- s/Str
     media-format :- s/Str
     media-notes :- (s/maybe s/Str)
     media-cameracheck :- s/Bool
     media-attention-needed :- s/Bool
     media-processed :- (s/maybe s/Bool)
     media-capture-timestamp :- org.joda.time.DateTime
     media-reference-quality :- (s/maybe s/Bool)
     trap-station-session-camera-id :- s/Int])

(s/defrecord Media
    [media-id :- s/Int
     media-created :- org.joda.time.DateTime
     media-updated :- org.joda.time.DateTime
     media-filename :- s/Str
     media-format :- s/Str
     media-notes :- (s/maybe s/Str)
     media-cameracheck :- s/Bool
     media-attention-needed :- s/Bool
     media-processed :- (s/maybe s/Bool)
     media-capture-timestamp :- org.joda.time.DateTime
     media-reference-quality :- s/Bool
     trap-station-session-camera-id :- s/Int])

(s/defn tmedia
  [{:keys [media-filename media-format media-notes media-cameracheck
           media-attention-needed media-processed media-capture-timestamp
           media-reference-quality trap-station-session-camera-id]}]
  (->TMedia media-filename media-format media-notes media-cameracheck
            media-attention-needed media-processed media-capture-timestamp
            (or media-reference-quality false) trap-station-session-camera-id))

(s/defn media
  [{:keys [media-id media-created media-updated media-filename media-format
           media-notes media-cameracheck media-attention-needed media-processed
           media-reference-quality media-capture-timestamp trap-station-session-camera-id]}]
  (->Media media-id media-created media-updated media-filename media-format
           media-notes media-cameracheck media-attention-needed media-processed
           media-capture-timestamp media-reference-quality trap-station-session-camera-id))

(s/defn get-all :- [Media]
  [state :- State
   id :- s/Int]
  (map media (db/with-db-keys state -get-all {:trap-station-session-camera-id id})))

(s/defn get-specific :- (s/maybe Media)
  [state :- State
   id :- s/Int]
  (some->> {:media-id id}
           (db/with-db-keys state -get-specific)
           first
           media))

(s/defn get-specific-by-filename :- (s/maybe Media)
  [state :- State
   filename :- s/Str]
  (some->> {:media-filename filename}
           (db/with-db-keys state -get-specific-by-filename)
           first
           media))

(s/defn create! :- Media
  [state :- State
   data :- TMedia]
  (let [record (db/with-db-keys state -create<! data)]
    (media (get-specific state (int (:1 record))))))

(s/defn update! :- Media
  [state :- State
   id :- s/Int
   data :- TMedia]
  (db/with-db-keys state -update! (merge data {:media-id id}))
  (media (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Num]
  (db/with-db-keys state -delete! {:media-id id})
  nil)

(s/defn update-processed-flag!
  [state :- State
   {:keys [media-id media-processed]}]
  (db/with-db-keys state -update-processed-flag! {:media-id media-id
                                                  :media-processed media-processed}))

(s/defn update-reference-quality-flag!
  [state :- State
   {:keys [media-id media-reference-quality]}]
  (db/with-db-keys state -update-reference-quality-flag!
    {:media-id media-id
     :media-reference-quality media-reference-quality}))

(s/defn update-media-flags!
  [state :- State
   {:keys [media-id media-attention-needed media-processed media-reference-quality]}]
  (db/with-db-keys state -update-media-flags! {:media-id media-id
                                               :media-reference-quality (or media-reference-quality false)
                                               :media-attention-needed media-attention-needed
                                               :media-processed media-processed}))

(s/defn read-media-file :- java.io.BufferedInputStream
  [state :- State
   filename :- s/Str
   variant :- (s/enum :thumb :preview :original)]
  (if-let [media (get-specific-by-filename state filename)]
    (io/input-stream
     (if (= variant :original)
       (io/file (str (config/get-media-path) SystemUtils/FILE_SEPARATOR
                     filename "."
                     (:media-format media)))
       (io/file (str (config/get-media-path) SystemUtils/FILE_SEPARATOR
                     (name variant) "-" filename ".png"))))))
