(ns camelot.routes
  (:require [compojure.core :refer [GET POST defroutes routes]]
            [clojure.java.io :as io]
            [compojure.route :as route]
            [camelot.handler.albums :as albums]
            [camelot.handler.settings :as settings]
            [camelot.handler.surveys :as surveys]
            [camelot.handler.sites :as sites]
            [camelot.handler.cameras :as cameras]
            [camelot.handler.camera-statuses :as camera-statuses]
            [camelot.handler.survey-sites :as survey-sites]
            [camelot.handler.trap-stations :as trap-stations]
            [camelot.handler.trap-station-sessions :as trap-station-sessions]
            [camelot.handler.trap-station-session-cameras :as trap-station-session-cameras]
            [camelot.handler.screens :as screens]
            [camelot.analysis.maxent :as maxent]))

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
          settings/routes
          albums/routes
          screens/routes
          surveys/routes
          sites/routes
          cameras/routes
          camera-statuses/routes
          survey-sites/routes
          trap-stations/routes
          trap-station-sessions/routes
          trap-station-session-cameras/routes))
