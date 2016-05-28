(ns camelot.handler.import
  (:require [camelot.processing.album :as a]
            [camelot.util.config :as conf]
            [camelot.util.application :as app]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [ring.util.response :as r]
            [camelot.handler.surveys :as surveys]
            [camelot.handler.survey-sites :as survey-sites]
            [camelot.handler.trap-stations :as trap-stations]
            [camelot.handler.trap-station-sessions :as trap-station-sessions]
            [camelot.handler.trap-station-session-cameras :as trap-station-session-cameras]
            [camelot.handler.media :as media]
            [camelot.util.rest :as rest]
            [clojure.edn :as edn]
            [camelot.processing.album :as album]
            [clojure.string :as str]
            [camelot.handler.sites :as sites]
            [camelot.handler.cameras :as cameras]
            [camelot.handler.camera-statuses :as camera-statuses]
            [clojure.java.io :as io]
            [camelot.util.config :as util.config]
            [camelot.util.java-file :as jf]))

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
  (let [surveys (canonicalise (surveys/get-all (app/gen-state (conf/config)))
                              :survey-id :survey-name)
        survey-sites (canonicalise (maybe-get survey-sites/get-all (:survey params))
                                :survey-site-id :site-name)
        trap-stations (canonicalise (maybe-get trap-stations/get-all (:survey-site params))
                                 :trap-station-id :trap-station-name)
        trap-station-sessions (canonicalise (maybe-get trap-station-sessions/get-all (:trap-station params))
                                         :trap-station-session-id :trap-station-session-label)
        trap-station-session-cameras (canonicalise (maybe-get trap-station-session-cameras/get-all
                                                (:trap-station-session params))
                                                :trap-station-session-camera-id
                                                :camera-name)]
    {:surveys surveys
     :survey-sites survey-sites
     :trap-stations trap-stations
     :trap-station-sessions trap-station-sessions
     :trap-station-session-cameras trap-station-session-cameras}))

(defn get-or-create-site
  [state sitename sample]
  (let [data {:site-name sitename}
        loc (:location sample)]
    (or (sites/get-specific-by-name state data)
        (sites/create! state (merge data
                                    {:site-sublocation (:sublocation loc)
                                     :site-city (:city loc)
                                     :site-state-province (:state loc)
                                     :site-country (:country loc)
                                     :site-notes "Auto-created by Camelot"})))))

(defn get-or-create-camera
  [state cameraname sample]
  (let [data {:camera-name cameraname}
        settings (:camera-settings sample)
        camera-status (first (camera-statuses/get-all state))]
    (or (cameras/get-specific-by-name state data)
        (cameras/create! state (merge data
                                      {:camera-status-id (:camera-status-id camera-status)
                                       :camera-make (:make settings)
                                       :camera-model (:model settings)
                                       :camera-notes "Auto-created by Camelot"})))))

(defn get-or-create-survey
  [state directory]
  (or (first (surveys/get-all state))
      (surveys/create! state {:survey-name "Initial survey"
                              :survey-directory directory
                              :survey-notes "Auto-created by Camelot"})))

(defn get-or-create-survey-site
  [state survey-id site-id]
  (let [data {:survey-id survey-id
              :site-id site-id}]
    (or (survey-sites/get-specific-by-site state data)
        (survey-sites/create! state data))))

(defn get-or-create-trap-station
  [state survey-site-id sample]
  (let [longitude (:gps-longitude (:location sample))
        latitude (:gps-latitude (:location sample))
        data {:survey-site-id survey-site-id
              :trap-station-name (str "Trap at " longitude ", " latitude)
              :trap-station-longitude longitude
              :trap-station-latitude latitude}]
    (or (trap-stations/get-specific-by-location state data)
        (trap-stations/create! state (merge data
                                            {:trap-station-notes "Auto-created by Camelot"
                                             :trap-station-altitude nil})))))

(defn get-or-create-trap-station-session
  [state trap-station-id start end]
  (let [data {:trap-station-id trap-station-id
              :trap-station-session-start-date start
              :trap-station-session-end-date end
              :trap-station-session-notes "Auto-created by Camelot"}]
    (or (trap-station-sessions/get-specific-by-dates state data)
        (trap-station-sessions/create! state data))))

(defn get-or-create-trap-station-session-camera
  [state trap-station-session-id camera-id folder-path]
  (let [data {:trap-station-session-id trap-station-session-id
              :camera-id camera-id
              :trap-station-session-camera-import-path folder-path}]
    (or (trap-station-session-cameras/get-specific-by-camera state data)
        (trap-station-session-cameras/create! state data))))

(defn media
  "Import media"
  [{:keys [folder trap-station-session-camera notes]}]
  (let [state (app/gen-state (conf/config))
        root-path (:root-path (:config state))
        albums (album/read-albums state root-path)
        full-path (str root-path folder)
        album (get albums (io/file full-path))
        sample (second (first (:photos album)))
        pps (prn sample)
        [_ sitename phase cameraname] (str/split folder #"/")
        survey (get-or-create-survey state root-path)
        site (get-or-create-site state sitename sample)
        camera (get-or-create-camera state cameraname sample)
        survey-site (get-or-create-survey-site state
                                               (:survey-id survey)
                                               (:site-id site))
        trap-station (get-or-create-trap-station state
                                                 (:survey-site-id survey-site)
                                                 sample)
        trap-station-session (get-or-create-trap-station-session state
                                                                 (:trap-station-id trap-station)
                                                                 (:datetime-start (:metadata album))
                                                                 (:datetime-end (:metadata album)))
        trap-station-session-camera (get-or-create-trap-station-session-camera state
                                                                               (:trap-station-session-id trap-station-session)
                                                                               (:camera-id camera)
                                                                               full-path)]
    (doseq [photo (vals (:photos album))]
      (let [filename (str (java.util.UUID/randomUUID)
                          (subs (:filename photo)
                                (- (count (:filename photo)) 4)))
            targetname (str (util.config/get-media-path) "/"
                            (str/lower-case filename))]
        (media/create! state {:media-capture-timestamp (:datetime photo)
                              :media-filename filename
                              :media-cameracheck (if (nil? (:species (first (:sightings photo))))
                                                   false
                                                   (not (nil? (re-matches #"(?i)\bcamera-check\b"
                                                                          (:species (first (:sightings photo)))))))
                              :media-attention-needed false
                              :media-notes notes
                              :trap-station-session-camera-id (:trap-station-session-camera-id trap-station-session-camera)})
        (io/make-parents targetname)
        (io/copy (io/file (str full-path "/" (:filename photo)))
                 (io/file targetname))))))

(def routes
  "Import routes"
  (context "/import" []
           (POST "/options" [data] (r/response (options data)))
           (POST "/media" [data] (r/response (media data)))))
