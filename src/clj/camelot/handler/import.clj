(ns camelot.handler.import
  (:require [camelot.import.album :as a]
            [camelot.util.config :as conf]
            [camelot.application :as app]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [ring.util.response :as r]
            [camelot.model.survey :as survey]
            [camelot.model.survey-site :as survey-site]
            [camelot.model.trap-station :as trap-station]
            [camelot.model.trap-station-session :as trap-station-session]
            [camelot.model.trap-station-session-camera :as trap-station-session-camera]
            [camelot.model.media :as media]
            [camelot.model.photo :as photo]
            [camelot.model.sighting :as sighting]
            [camelot.model.species :as species]
            [camelot.model.site :as site]
            [camelot.model.camera :as camera]
            [camelot.model.camera-status :as camera-status]
            [camelot.import.validation :as validation]
            [clojure.edn :as edn]
            [camelot.import.album :as album]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [camelot.util.config :as util.config]
            [camelot.util.java-file :as jf]
            [camelot.db :as db])
  (:import [org.apache.commons.lang3 SystemUtils]))

(defn- canonicalise
  [resources vkey desckey]
  (map #(assoc %
               :vkey (get % vkey)
               :desc (get % desckey))
       resources))

(defn- maybe-get
  [resource v]
  (let [state (app/gen-state (conf/config))]
    (if v
      (resource state v)
      [])))

(defn options
  "Return all albums for the current configuration."
  [params]
  (let [surveys (canonicalise (survey/get-all (app/gen-state (conf/config)))
                              :survey-id :survey-name)
        survey-sites (canonicalise (maybe-get survey-site/get-all (:survey params))
                                   :survey-site-id :site-name)
        trap-stations (canonicalise (maybe-get trap-station/get-all (:survey-site params))
                                    :trap-station-id :trap-station-name)
        trap-sessions (canonicalise (maybe-get trap-station-session/get-all (:trap-station params))
                                    :trap-station-session-id :trap-station-session-label)
        trap-cameras (canonicalise (maybe-get trap-station-session-camera/get-all
                                              (:trap-station-session params))
                                   :trap-station-session-camera-id
                                   :camera-name)]
    {:surveys surveys
     :survey-sites survey-sites
     :trap-stations trap-stations
     :trap-station-sessions trap-sessions
     :trap-station-session-cameras trap-cameras}))

(defn- get-or-create-site
  [state sitename sample]
  (let [data {:site-name sitename}
        loc (:location sample)]
    (or (site/get-specific-by-name state data)
        (site/create! state (merge data
                                    {:site-sublocation (:sublocation loc)
                                     :site-city (:city loc)
                                     :site-state-province (:state-province loc)
                                     :site-country (:country loc)
                                     :site-notes "Auto-created by Camelot"})))))

(defn- get-or-create-camera
  [state cameraname sample]
  (let [data {:camera-name cameraname}
        settings (:camera-settings sample)
        camera-status (first (camera-status/get-all state))]
    (or (camera/get-specific-by-name state data)
        (camera/create! state (merge data
                                      {:camera-status-id (:camera-status-id camera-status)
                                       :camera-make (:make (:camera sample))
                                       :camera-model (:model (:camera sample))
                                       :camera-notes "Auto-created by Camelot"})))))

(defn- get-or-create-survey
  [state directory]
  (or (first (survey/get-all state))
      (survey/create! state {:survey-name "Initial survey"
                              :survey-directory directory
                              :survey-sampling-point-density nil
                              :survey-notes "Auto-created by Camelot"})))

(defn- get-or-create-survey-site
  [state survey site]
  (let [data {:survey-id (:survey-id survey)
              :site-id (:site-id site)}]
    (or (survey-site/get-specific-by-site state data)
        (survey-site/create! state data))))

(defn- get-or-create-trap-station
  [state sample survey-site]
  (let [longitude (:gps-longitude (:location sample))
        latitude (:gps-latitude (:location sample))
        data {:survey-site-id (:survey-site-id survey-site)
              :trap-station-name (str "Trap at " longitude ", " latitude)
              :trap-station-longitude longitude
              :trap-station-latitude latitude}]
    (or (trap-station/get-specific-by-location state data)
        (trap-station/create! state (merge data
                                            {:trap-station-notes "Auto-created by Camelot"
                                             :trap-station-altitude nil})))))

(defn- get-or-create-trap-session
  [state album trap-station]
  (let [data {:trap-station-id (:trap-station-id trap-station)
              :trap-station-session-start-date (:datetime-start (:metadata album))
              :trap-station-session-end-date (:datetime-end (:metadata album))
              :trap-station-session-notes "Auto-created by Camelot"}]
    (or (trap-station-session/get-specific-by-dates state data)
        (trap-station-session/create! state data))))

(defn- get-or-create-trap-camera
  [state camera folder-path trap-station-session]
  (let [data {:trap-station-session-id (:trap-station-session-id trap-station-session)
              :camera-id (:camera-id camera)
              :trap-station-session-camera-import-path folder-path}]
    (or (trap-station-session-camera/get-specific-by-import-path state folder-path)
        (trap-station-session-camera/create! state data))))

(defn- get-or-create-species
  [state sighting]
  (let [spp (str/lower-case (:species sighting))]
    (or (species/get-specific-by-scientific-name state spp)
        (species/create! state {:species-scientific-name spp
                                :species-common-name "N/A"
                                :species-notes "Auto-created by Camelot"}))))

(defn- create-photo
  [state media-id camset]
  (photo/create! state {:photo-iso-setting (:iso camset)
                         :photo-aperture-setting (:aperture camset)
                         :photo-exposure-value (:exposure camset)
                         :photo-flash-setting (:flash camset)
                         :photo-focal-length (:focal-length camset)
                         :photo-fnumber-setting (:fstop camset)
                         :photo-orientation (:orientation camset)
                         :photo-resolution-x (:resolution-x camset)
                         :photo-resolution-y (:resolution-y camset)
                         :media-id media-id}))

(defn- create-sightings
  [state media-id sightings]
  (doseq [sighting sightings]
    (when-not (re-find validation/sighting-quantity-exclusions-re (:species sighting))
      (let [species (get-or-create-species state sighting)]
        (sighting/create! state {:sighting-quantity (:quantity sighting)
                                  :species-id (:species-id species)
                                  :media-id media-id})))))

(def cameracheck-re
  "Regexp for determining whether a sighting species is actually a camera check."
  #"(?i)\bcamera-check\b")

(defn- is-cameracheck?
  [photo]
  (let [spp (:species (first (:sightings photo)))]
    (= (and spp (re-matches cameracheck-re spp)) true)))

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

(defn- create-media
  [state photo filename notes trap-camera-id]
  (media/create! state {:media-capture-timestamp (:datetime photo)
                        :media-filename (str/lower-case filename)
                        :media-cameracheck (is-cameracheck? photo)
                        :media-attention-needed false
                        :media-notes notes
                        :trap-station-session-camera-id trap-camera-id}))

(defn- get-album
  [state root-path path]
  (-> (album/read-albums state root-path)
      (get (io/file path))))

(defn- import-media-for-camera
  [state notes full-path trap-camera photos]
  (doseq [photo photos]
    (let [filename (get-unique-filename (:filename photo))
          camset (:settings photo)
          photopath (str full-path SystemUtils/FILE_SEPARATOR (:filename photo))
          targetname (str (util.config/get-media-path) SystemUtils/FILE_SEPARATOR (str/lower-case filename))
          media (create-media state photo filename notes trap-camera)]
      (create-photo state (:media-id media) camset)
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
          survey (get-or-create-survey state root-path)
          camera (get-or-create-camera state cameraname sample)
          trap-camera (->> (get-or-create-site state sitename sample)
                           (get-or-create-survey-site state survey)
                           (get-or-create-trap-station state sample)
                           (get-or-create-trap-session state album)
                           (get-or-create-trap-camera state camera folder))]
      (import-media-for-camera state notes full-path
                               (:trap-station-session-camera-id trap-camera)
                               (vals (:photos album))))))

(def routes
  "Import routes"
  (context "/import" []
           (POST "/options" [data] (r/response (options data)))
           (POST "/media" [data] (r/response (media data)))))
