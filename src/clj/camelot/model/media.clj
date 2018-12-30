(ns camelot.model.media
  "Media models and data access."
  (:require
   [schema.core :as sch]
   [clojure.spec.alpha :as s]
   [camelot.spec.system :as sysspec]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.state :as state]
   [camelot.util.db :as db]
   [clojure.java.io :as io]
   [camelot.util.file :as file]
   [clojure.string :as str]
   [clj-time.format :as tf]
   [clojure.tools.logging :as log])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(def query (db/with-db-keys :media))

(sch/defrecord TMedia
    [media-filename :- sch/Str
     media-format :- sch/Str
     media-notes :- (sch/maybe sch/Str)
     media-cameracheck :- sch/Bool
     media-attention-needed :- sch/Bool
     media-processed :- (sch/maybe sch/Bool)
     media-capture-timestamp :- org.joda.time.DateTime
     media-reference-quality :- (sch/maybe sch/Bool)
     trap-station-session-camera-id :- sch/Int]
  {sch/Any sch/Any})

(sch/defrecord Media
    [media-id :- sch/Int
     media-created :- org.joda.time.DateTime
     media-updated :- org.joda.time.DateTime
     media-filename :- sch/Str
     media-format :- sch/Str
     media-notes :- (sch/maybe sch/Str)
     media-cameracheck :- sch/Bool
     media-attention-needed :- sch/Bool
     media-processed :- (sch/maybe sch/Bool)
     media-capture-timestamp :- org.joda.time.DateTime
     media-reference-quality :- sch/Bool
     trap-station-session-camera-id :- sch/Int
     media-capture-timestamp-label :- sch/Str]
  {sch/Any sch/Any})

(defn tmedia
  [ks]
  (map->TMedia (update ks :media-reference-quality #(or % false))))

(defn media
  [ks]
  (map->Media (assoc ks :media-capture-timestamp-label
                     (tf/unparse (tf/formatters :mysql) (:media-capture-timestamp ks)))))

(sch/defn get-all
  [state id]
  (map media (query state :get-all {:trap-station-session-camera-id id})))

(sch/defn get-all* :- [Media]
  [state :- State]
  (map media (query state :get-all*)))

(sch/defn get-all-files-by-survey :- [sch/Str]
  [state :- State
   id :- sch/Int]
  (map :media-file (query state :get-all-files-by-survey {:survey-id id})))

(defn get-with-ids
  [state media-ids]
  (map media (query state :get-with-ids {:media-ids media-ids})))

(sch/defn get-all-files-by-survey-site :- [sch/Str]
  [state :- State
   id :- sch/Int]
  (map :media-file (query state :get-all-files-by-survey-site {:survey-site-id id})))

(sch/defn get-all-files-by-site :- [sch/Str]
  [state :- State
   id :- sch/Int]
  (map :media-file (query state :get-all-files-by-site {:site-id id})))

(sch/defn get-all-files-by-camera :- [sch/Str]
  [state :- State
   id :- sch/Int]
  (map :media-file (query state :get-all-files-by-camera {:camera-id id})))

(sch/defn get-all-files-by-trap-station :- [sch/Str]
  [state :- State
   id :- sch/Int]
  (map :media-file (query state :get-all-files-by-trap-station {:trap-station-id id})))

(sch/defn get-all-files-by-trap-station-session :- [sch/Str]
  [state :- State
   id :- sch/Int]
  (map :media-file (query state :get-all-files-by-trap-station-session {:trap-station-session-id id})))

(sch/defn get-all-files-by-trap-station-session-camera :- [sch/Str]
  [state :- State
   id :- sch/Int]
  (map :media-file (query state :get-all-files-by-trap-station-session-camera {:trap-station-session-camera-id id})))

(sch/defn get-specific :- (sch/maybe Media)
  [state :- State
   id :- sch/Int]
  (some->> {:media-id id}
           (query state :get-specific)
           first
           media))

(sch/defn get-specific-by-filename :- (sch/maybe Media)
  [state :- State
   filename :- sch/Str]
  (some->> {:media-filename filename}
           (query state :get-specific-by-filename)
           first
           media))

(defn create!
  [state data]
  (let [record (query state :create<! data)]
    (media (get-specific state (int (:1 record))))))

(sch/defn update! :- Media
  [state :- State
   id :- sch/Int
   data :- TMedia]
  (query state :update! (merge data {:media-id id}))
  (media (get-specific state id)))

(defn path-to-file
  "Return the full path to an image file."
  [state variant filename orig-format]
  (let [mpath (state/lookup-path state :media)
        prefix (if (= variant :original) "" (str (name variant) "-"))
        fmt (if (= variant :original) orig-format "png")]
    (io/file mpath (apply str (take 2 filename))
             (str prefix filename "." fmt))))

(defn path-to-media
  "Return the path to a file given a media record"
  [state variant media]
  (path-to-file state variant (:media-filename media) (:media-format media)))

(sch/defn delete-file!
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

(s/fdef delete-files!
        :args (s/cat :state ::sysspec/state
                     :files (s/coll-of string?))
        :ret nil?)

(sch/defn delete!
  "Delete the file with the given ID."
  [state :- State
   id :- sch/Num]
  (if-let [media (get-specific state id)]
    (do
      (query state :delete! {:media-id id})
      (dorun (map #(file/delete (path-to-media state % media))
                  [:original :thumb]))))
  nil)

(sch/defn delete-with-ids!
  [state :- State
   media-ids]
  (dorun (map (partial delete! state) media-ids))
  nil)

(sch/defn update-processed-flag!
  [state :- State
   {:keys [media-id media-processed]}]
  (query state :update-processed-flag! {:media-id media-id
                                                  :media-processed media-processed}))

(sch/defn update-reference-quality-flag!
  [state :- State
   {:keys [media-id media-reference-quality]}]
  (query state :update-reference-quality-flag!
    {:media-id media-id
     :media-reference-quality media-reference-quality}))

(sch/defn update-media-flags!
  [state :- State
   {:keys [media-id media-attention-needed media-processed media-reference-quality media-cameracheck]}]
  (query state :update-media-flags! {:media-id media-id
                                               :media-reference-quality (or media-reference-quality false)
                                               :media-attention-needed media-attention-needed
                                               :media-cameracheck (or media-cameracheck false)
                                               :media-processed media-processed}))

(sch/defn read-media-file :- (sch/maybe java.io.BufferedInputStream)
  [state :- State
   filename :- sch/Str
   variant :- (sch/enum :thumb :preview :original)]
  (if-let [media (get-specific-by-filename state filename)]
    (let [format (:media-format media)
          fpath (path-to-media state variant media)]
      (if (and (file/exists? fpath) (file/readable? fpath) (file/file? fpath))
        (io/input-stream fpath)
        (log/warn "File not found: " (file/get-path fpath))))))
