(ns camelot.handler.import
  (:require
   [camelot.import.album :as a]
   [camelot.util.config :as conf]
   [camelot.app.state :as state]
   [compojure.core :refer [ANY context DELETE GET POST PUT]]
   [ring.util.response :as r]
   [camelot.import.validation :as validation]
   [clojure.edn :as edn]
   [camelot.import.album :as album]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [camelot.util.config :as util.config]
   [camelot.util.java-file :as jf]
   [camelot.util.file :as file-util]
   [camelot.db.core :as db]
   [mikera.image.core :as image]
   [camelot.import.db :as im.db])
  (:import (org.apache.commons.lang3 SystemUtils)))

(defn- save-pathname
  [f dest]
  (io/make-parents dest)
  (let [d (io/file dest)]
    (if (jf/exists? d)
      (throw (java.io.IOException. (format "copy-pathname: file '%s' already exists", dest)))
      (f dest))))

(def image-variants
  {"thumb-" 256
   "preview-" 768
   "" nil})

(defn- store-original
  [src dest]
  (save-pathname #(io/copy (io/file src) (io/file %)) dest))

(defn- store-variant
  [^java.awt.image.BufferedImage image dest]
  (save-pathname #(image/save image % :quality 0.7 :progressive false) dest))

(defn- create-variant
  [path target width]
  (let [img (image/load-image path)]
    (store-variant (image/resize img width) target)))

(defn- create-image
  [path file-basename extension variant width]
  (let [target (str (util.config/get-media-path) SystemUtils/FILE_SEPARATOR
                    variant (str/lower-case file-basename))]
    (if width
      ;; Always create variants as .png; OpenJDK cannot write .jpg
      (create-variant path (str target ".png") width)
      (store-original path (str target "." extension)))))

(defn create-image-files
  [path filename extension]
  (dorun (map (fn [[k v]] (create-image path filename extension k v)) image-variants)))

(defn- get-album
  [state root-path path]
  (-> (album/read-albums state root-path)
      (get (io/file path))))

(defn- create-sightings
  [state media-id sightings]
  (doseq [sighting sightings]
    (when-not (re-find validation/sighting-quantity-exclusions-re (:species sighting))
      (im.db/create-sighting! state media-id sighting))))

(defn- import-media-for-camera
  [state notes full-path trap-camera photos]
  (doseq [photo photos]
    (let [filename (java.util.UUID/randomUUID)
          fmt (str/lower-case (second (re-find #".*\.(.+?)$" (:filename photo))))
          camset (:settings photo)
          photopath (str full-path SystemUtils/FILE_SEPARATOR (:filename photo))
          attn (some? (some #(re-find #"(?i)unidentified" (:species %)) (:sightings photo)))
          media (im.db/create-media! state photo filename fmt notes attn trap-camera)]
      (im.db/create-photo! state (:media-id media) camset)
      (create-sightings state (:media-id media) (:sightings photo))
      (create-image-files photopath filename fmt))))

(defn media
  "Import media"
  [{:keys [folder session-camera-id notes]}]
  (db/with-transaction [state (state/gen-state (conf/config))]
    (let [[_ sitename _phase cameraname] (file-util/rel-path-components folder)
          root-path (:root-path (:config state))
          full-path (str root-path folder)
          album (get-album state root-path full-path)
          sample (second (first (:photos album)))
          survey (im.db/get-or-create-survey! state root-path)
          camera (im.db/get-or-create-camera! state cameraname sample)
          trap-camera (->> (im.db/get-or-create-site! state sitename sample)
                           (im.db/get-or-create-survey-site! state survey)
                           (im.db/get-or-create-trap-station! state sample)
                           (im.db/get-or-create-trap-session! state album)
                           (im.db/get-or-create-trap-camera! state camera folder))]
      (import-media-for-camera state notes full-path
                               (:trap-station-session-camera-id trap-camera)
                               (vals (:photos album))))))
