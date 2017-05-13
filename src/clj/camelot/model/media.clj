(ns camelot.model.media
  "Media models and data access."
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.system.state :refer [State]]
   [camelot.util.db :as db]
   [clojure.java.io :as io]
   [camelot.util.file :as file]
   [clojure.string :as str]
   [clj-time.format :as tf]
   [clojure.tools.logging :as log])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(sql/defqueries "sql/media.sql")

(s/defrecord TMedia
    [media-filename :- s/Str
     media-format :- s/Str
     media-notes :- (s/maybe s/Str)
     media-cameracheck :- s/Bool
     media-attention-needed :- s/Bool
     media-processed :- (s/maybe s/Bool)
     media-capture-timestamp :- org.joda.time.DateTime
     media-reference-quality :- (s/maybe s/Bool)
     trap-station-session-camera-id :- s/Int]
  {s/Any s/Any})

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
     trap-station-session-camera-id :- s/Int
     media-capture-timestamp-label :- s/Str]
  {s/Any s/Any})

(defn tmedia
  [ks]
  (map->TMedia (update ks :media-reference-quality #(or % false))))

(defn media
  [ks]
  (map->Media (assoc ks :media-capture-timestamp-label
                     (tf/unparse (tf/formatters :mysql) (:media-capture-timestamp ks)))))

(s/defn get-all
  [state id]
  (map media (db/with-db-keys state -get-all {:trap-station-session-camera-id id})))

(s/defn get-all* :- [Media]
  [state :- State]
  (map media (db/clj-keys (db/with-connection state -get-all*))))

(s/defn get-all-files-by-survey :- [s/Str]
  [state :- State
   id :- s/Int]
  (map :media-file (db/with-db-keys state -get-all-files-by-survey {:survey-id id})))

(s/defn get-all-files-by-survey-site :- [s/Str]
  [state :- State
   id :- s/Int]
  (map :media-file (db/with-db-keys state -get-all-files-by-survey-site {:survey-site-id id})))

(s/defn get-all-files-by-site :- [s/Str]
  [state :- State
   id :- s/Int]
  (map :media-file (db/with-db-keys state -get-all-files-by-site {:site-id id})))

(s/defn get-all-files-by-camera :- [s/Str]
  [state :- State
   id :- s/Int]
  (map :media-file (db/with-db-keys state -get-all-files-by-camera {:camera-id id})))

(s/defn get-all-files-by-trap-station :- [s/Str]
  [state :- State
   id :- s/Int]
  (map :media-file (db/with-db-keys state -get-all-files-by-trap-station {:trap-station-id id})))

(s/defn get-all-files-by-trap-station-session :- [s/Str]
  [state :- State
   id :- s/Int]
  (map :media-file (db/with-db-keys state -get-all-files-by-trap-station-session {:trap-station-session-id id})))

(s/defn get-all-files-by-trap-station-session-camera :- [s/Str]
  [state :- State
   id :- s/Int]
  (map :media-file (db/with-db-keys state -get-all-files-by-trap-station-session-camera {:trap-station-session-camera-id id})))

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

(defn create!
  [state data]
  (let [record (db/with-db-keys state -create<! data)]
    (media (get-specific state (int (:1 record))))))

(s/defn update! :- Media
  [state :- State
   id :- s/Int
   data :- TMedia]
  (db/with-db-keys state -update! (merge data {:media-id id}))
  (media (get-specific state id)))

(defn path-to-file
  "Return the full path to an image file."
  [state variant filename orig-format]
  (let [mpath (get-in state [:config :path :media])
        prefix (if (= variant :original) "" (str (name variant) "-"))
        fmt (if (= variant :original) orig-format "png")]
    (io/file mpath (apply str (take 2 filename))
             (str prefix filename "." fmt))))

(defn path-to-media
  "Return the path to a file given a media record"
  [state variant media]
  (path-to-file state variant (:media-filename media) (:media-format media)))

(s/defn delete-file!
  "Delete a file with the given name from the media directory, along with any
  associated variants."
  [state filename]
  (let [file (io/file filename)]
    (map #(file/delete (path-to-file state %
                                     (file/basename file #"\.(png|jpg)$")
                                     (file/extension filename)))
         [:original :thumb])))

(defn delete-files!
  "Delete the images associated with each image file."
  [state files]
  (doall (mapcat (partial delete-file! state) files)))

(s/defn delete!
  "Delete the file with the given ID."
  [state :- State
   id :- s/Num]
  (if-let [media (get-specific state id)]
    (do
      (db/with-db-keys state -delete! {:media-id id})
      (dorun (map #(file/delete (path-to-media state % media))
                  [:original :thumb]))))
  nil)

(s/defn delete-with-ids!
  [state :- State
   media-ids]
  (dorun (map (partial delete! state) media-ids))
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
   {:keys [media-id media-attention-needed media-processed media-reference-quality media-cameracheck]}]
  (db/with-db-keys state -update-media-flags! {:media-id media-id
                                               :media-reference-quality (or media-reference-quality false)
                                               :media-attention-needed media-attention-needed
                                               :media-cameracheck (or media-cameracheck false)
                                               :media-processed media-processed}))

(s/defn read-media-file :- (s/maybe java.io.BufferedInputStream)
  [state :- State
   filename :- s/Str
   variant :- (s/enum :thumb :preview :original)]
  (if-let [media (get-specific-by-filename state filename)]
    (let [format (:media-format media)
          fpath (path-to-media state variant media)]
      (if (and (file/exists? fpath) (file/readable? fpath) (file/file? fpath))
        (io/input-stream fpath)
        (log/warn "File not found: " (file/get-path fpath))))))
