(ns camelot.import.db
  (:require [camelot.model.survey :as survey]
            [camelot.model.survey-site :as survey-site]
            [camelot.model.trap-station :as trap-station]
            [camelot.model.trap-station-session :as trap-station-session]
            [camelot.model.trap-station-session-camera :as trap-station-session-camera]
            [camelot.model.media :as media]
            [camelot.model.photo :as photo]
            [camelot.model.sighting :as sighting]
            [camelot.model.taxonomy :as taxonomy]
            [camelot.model.site :as site]
            [camelot.model.camera :as camera]
            [camelot.model.camera-status :as camera-status]
            [clojure.string :as str]
            [camelot.application :as app]
            [camelot.util.config :as conf]
            [clojure.edn :as edn]))

(def default-survey-name "Initial survey")

(def import-note "Auto-created by Camelot")

(defn- canonicalise
  [resources vkey desckey]
  (map #(assoc %
               :vkey (get % vkey)
               :desc (get % desckey))
       resources))

(defn- maybe-get
  [resource v]
  (if v
    (let [state (app/gen-state (conf/config))]
      (resource state v))
    []))

(def cameracheck-re
  "Regexp for determining whether a sighting species is actually a camera check."
  #"(?i)\bcamera-check\b")

(defn- is-cameracheck?
  [photo]
  (let [spp (:species (first (:sightings photo)))]
    (= (and spp (re-matches cameracheck-re spp)) true)))

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

(defn get-or-create-site!
  [state sitename sample]
  (let [loc (:location sample)
        tsite (site/tsite {:site-name sitename
                           :site-sublocation (:sublocation loc)
                           :site-city (:city loc)
                           :site-state-province (:state-province loc)
                           :site-country (:country loc)
                           :site-area nil
                           :site-notes import-note})]
    (site/get-or-create! state tsite)))

(defn get-or-create-camera!
  [state cameraname sample]
  (let [settings (:camera-settings sample)
        camera-status (first (camera-status/get-all state))]
    (camera/get-or-create!
     state
     (camera/tcamera {:camera-name cameraname
                      :camera-status-id (:camera-status-id camera-status)
                      :camera-make (:make (:camera sample))
                      :camera-model (:model (:camera sample))
                      :camera-notes import-note}))))

(defn get-or-create-survey!
  [state directory]
  (survey/get-or-create!
   state
   (survey/tsurvey {:survey-name default-survey-name
                    :survey-directory directory
                    :survey-sampling-point-density nil
                    :survey-notes import-note})))

(defn get-or-create-survey-site!
  [state survey site]
  (survey-site/get-or-create!
   state
   (survey-site/tsurvey-site (merge
                              (select-keys survey [:survey-id])
                              (select-keys site [:site-id])))))

(defn get-or-create-trap-station!
  [state sample survey-site]
  (let [longitude (:gps-longitude (:location sample))
        latitude (:gps-latitude (:location sample))
        altitude (:gps-altitude (:location sample))]
    (trap-station/get-or-create!
     state
     (trap-station/ttrap-station
      {:survey-site-id (:survey-site-id survey-site)
       :trap-station-name (str "Trap at " longitude ", " latitude)
       :trap-station-longitude longitude
       :trap-station-latitude latitude
       :trap-station-notes import-note
       :trap-station-altitude (and altitude (edn/read-string altitude))}))))

(defn get-or-create-trap-session!
  [state album trap-station]
  (trap-station-session/get-or-create!
   state
   (trap-station-session/ttrap-station-session
    {:trap-station-id (:trap-station-id trap-station)
     :trap-station-session-start-date (:datetime-start (:metadata album))
     :trap-station-session-end-date (:datetime-end (:metadata album))
     :trap-station-session-notes import-note})))

(defn get-or-create-trap-camera!
  [state camera folder-path trap-station-session]
  (trap-station-session-camera/get-or-create!
   state
   (trap-station-session-camera/ttrap-station-session-camera
    {:trap-station-session-id (:trap-station-session-id trap-station-session)
     :camera-id (:camera-id camera)
     :trap-station-session-camera-import-path folder-path})))

(defn get-or-create-taxonomy!
  [state sighting]
  (let [spp (:species sighting)
        name-parts (str/split spp #" ")]
    (taxonomy/get-or-create!
     state
     (taxonomy/ttaxonomy
      (if (= (count name-parts) 1)
        {:taxonomy-species spp
         :taxonomy-common-name "N/A"
         :taxonomy-notes import-note}
        {:taxonomy-genus (first name-parts)
         :taxonomy-species (str/join " " (rest name-parts))
         :taxonomy-common-name "N/A"
         :taxonomy-notes import-note})))))

(defn create-media!
  [state photo filename fmt notes attn trap-camera-id]
  (media/create!
   state
   (media/tmedia
    {:media-capture-timestamp (:datetime photo)
     :media-filename (str/lower-case filename)
     :media-format (str/lower-case fmt)
     :media-cameracheck (is-cameracheck? photo)
     :media-attention-needed attn
     :media-processed true
     :media-notes notes
     :trap-station-session-camera-id trap-camera-id})))

(defn create-photo!
  [state media-id camset]
  (photo/create!
   state
   (photo/tphoto
    {:photo-iso-setting (:iso camset)
     :photo-exposure-value (:exposure camset)
     :photo-flash-setting (:flash camset)
     :photo-focal-length (:focal-length camset)
     :photo-fnumber-setting (:fstop camset)
     :photo-orientation (:orientation camset)
     :photo-resolution-x (:resolution-x camset)
     :photo-resolution-y (:resolution-y camset)
     :media-id media-id})))

(defn create-sighting!
  [state media-id sighting]
  (let [taxonomy (get-or-create-taxonomy! state sighting)]
    (sighting/create!
     state
     (sighting/tsighting
      {:sighting-quantity (:quantity sighting)
       :taxonomy-id (:taxonomy-id taxonomy)
       :media-id media-id}))))
