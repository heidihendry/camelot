(ns camelot.routes
  (:require [camelot.handler
             [albums :as albums]
             [config :as config]
             [application :as app]
             [camera-statuses :as camera-statuses]
             [cameras :as cameras]
             [maxent :as maxent]
             [screens :as screens]
             [sites :as sites]
             [photos :as photos]
             [media :as media]
             [summary-statistics-report :as summary-statistics-report]
             [trap-station-report :as trap-station-report]
             [survey-site-report :as survey-site-report]
             [sightings :as sightings]
             [survey-sites :as survey-sites]
             [surveys :as surveys]
             [trap-station-session-cameras :as trap-station-session-cameras]
             [raw-data-export :as raw-data-export]
             [species-statistics-report :as species-statistics-report]
             [trap-station-sessions :as trap-station-sessions]
             [trap-stations :as trap-stations]
             [import :as import]]
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
          maxent/routes
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
          summary-statistics-report/routes
          species-statistics-report/routes
          raw-data-export/routes
          survey-site-report/routes
          trap-station-report/routes
          survey-sites/routes
          trap-stations/routes
          trap-station-sessions/routes
          trap-station-session-cameras/routes))
