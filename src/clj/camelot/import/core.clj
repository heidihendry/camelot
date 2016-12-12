(ns camelot.import.core
  (:require
   [camelot.app.state :as state]
   [camelot.db.core :as db]
   [camelot.import.album :as album]
   [camelot.import.db :as im.db]
   [camelot.import.validation :as validation]
   [camelot.util.file :as file]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [compojure.core :refer [ANY context DELETE GET POST PUT]]
   [mikera.image.core :as image]
   [ring.util.response :as r])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(defn- save-pathname
  [f dest]
  (io/make-parents dest)
  (let [d (file/->file dest)]
    (if (file/exists? d)
      (throw (java.io.IOException. (format "copy-pathname: file '%s' already exists", dest)))
      (f dest))))

(def image-variants
  {"thumb-" 256
   "preview-" 768
   "" nil})

(defn- store-original
  [src dest]
  (save-pathname #(io/copy (file/->file src) (file/->file %)) dest))

(defn- store-variant
  [^java.awt.image.BufferedImage image dest]
  (save-pathname #(image/save image % :quality 0.7 :progressive false) dest))

(defn- create-variant
  [path target width]
  (let [img (image/load-image path)]
    (store-variant (image/resize img width) target)))

(defn- create-image
  [path file-basename extension variant width]
  (let [target (str (state/get-media-path) SystemUtils/FILE_SEPARATOR
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
      (get (file/->file path))))

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
  [state {:keys [folder session-camera-id notes]}]
  (db/with-transaction [s state]
    (let [[_ sitename _phase cameraname] (file/rel-path-components folder)
          root-path (:root-path (:config s))
          full-path (str root-path folder)
          album (get-album s root-path full-path)
          sample (second (first (:photos album)))
          survey (im.db/get-or-create-survey! s root-path)
          camera (im.db/get-or-create-camera! s cameraname sample)
          trap-camera (->> (im.db/get-or-create-site! s sitename sample)
                           (im.db/get-or-create-survey-site! s survey)
                           (im.db/get-or-create-trap-station! s sample)
                           (im.db/get-or-create-trap-session! s album)
                           (im.db/get-or-create-trap-camera! s camera folder))]
      (import-media-for-camera s notes full-path
                               (:trap-station-session-camera-id trap-camera)
                               (vals (:photos album))))))
