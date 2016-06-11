(ns camelot.handler.import
  (:require [camelot.import.album :as a]
            [camelot.util.config :as conf]
            [camelot.application :as app]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [ring.util.response :as r]
            [camelot.import.validation :as validation]
            [clojure.edn :as edn]
            [camelot.import.album :as album]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [camelot.util.config :as util.config]
            [camelot.util.java-file :as jf]
            [camelot.db :as db]
            [camelot.import.db :as im.db])
  (:import [org.apache.commons.lang3 SystemUtils]))

(defn- get-unique-filename
  [filename]
  (str (java.util.UUID/randomUUID)
       (subs filename (- (count filename) 4))))

(defn- copy-pathname
  [src dest]
  (io/make-parents dest)
  (let [d (io/file dest)]
    (if (jf/exists? d)
      (throw (java.io.IOException. (format "copy-pathname: file '%s' already exists", dest)))
      (io/copy (io/file src) d))))

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
    (let [filename (get-unique-filename (:filename photo))
          camset (:settings photo)
          photopath (str full-path SystemUtils/FILE_SEPARATOR (:filename photo))
          targetname (str (util.config/get-media-path) SystemUtils/FILE_SEPARATOR
                          (str/lower-case filename))
          media (im.db/create-media! state photo filename notes trap-camera)]
      (im.db/create-photo! state (:media-id media) camset)
      (create-sightings state (:media-id media) (:sightings photo))
      (copy-pathname photopath targetname))))

(defn- path-separator-re
  []
  (if SystemUtils/IS_OS_WINDOWS
    #"\\"
    #"/"))

(defn media
  "Import media"
  [{:keys [folder session-camera-id notes]}]
  (db/with-transaction [state (app/gen-state (conf/config))]
    (let [[_ sitename cameraname] (str/split folder (path-separator-re))
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
