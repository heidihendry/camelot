(ns camelot.routes
  (:require [camelot.handler
             [albums :as albums]
             [config :as config]
             [application :as app]
             [camera-statuses :as camera-statuses]
             [cameras :as cameras]
             [screens :as screens]
             [sites :as sites]
             [photos :as photos]
             [media :as media]
             [sightings :as sightings]
             [survey-sites :as survey-sites]
             [surveys :as surveys]
             [trap-station-session-cameras :as trap-station-session-cameras]
             [trap-station-sessions :as trap-station-sessions]
             [trap-stations :as trap-stations]
             [import :as import]]
            [camelot.report
             [raw-data-export :as r.raw-data-export]
             [maxent :as r.maxent]
             [summary-statistics :as r.summary-statistics]
             [trap-station :as r.trap-station]
             [survey-site :as r.survey-site]
             [species-statistics :as r.species-statistics]]
            [clojure.java.io :as io]
            [compojure
             [core :refer [defroutes GET POST routes]]
             [route :as route]]
            [camelot.handler.species :as species]))

(defn- retrieve-index
  "Return a response for index.html"
  []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "public/index.html"))})

(defroutes misc-routes
  "Miscellaneous application routes."
  (GET "/" _ (retrieve-index))
  (POST "/quit" [] (System/exit 0))
  (route/resources "/"))

(def app-routes
  "All application routes."
  (routes misc-routes
          app/routes
          config/routes
          albums/routes
          screens/routes
          species/routes
          import/routes
          surveys/routes
          sites/routes
          cameras/routes
          media/routes
          photos/routes
          sightings/routes
          camera-statuses/routes
          survey-sites/routes
          trap-stations/routes
          trap-station-sessions/routes
          trap-station-session-cameras/routes
          r.raw-data-export/routes
          r.maxent/routes
          r.summary-statistics/routes
          r.species-statistics/routes
          r.survey-site/routes
          r.trap-station/routes))
