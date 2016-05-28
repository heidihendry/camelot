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
            [camelot.util.rest :as rest]
            [clojure.edn :as edn]))

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

(def routes
  "Import routes"
  (context "/import" []
           (POST "/options" [data] (r/response (options data)))))
