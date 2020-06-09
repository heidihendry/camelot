(ns camelot.http.core
  (:require
   [compojure.core :refer [defroutes]]
   [compojure.route :as route]
   [camelot.http.app :as app]
   [camelot.http.camera :as camera]
   [camelot.http.camera-status :as camera-status]
   [camelot.http.camera-deployment :as camera-deployment]
   [camelot.http.dataset :as dataset]
   [camelot.http.deployment :as deployment]
   [camelot.http.detector :as detector]
   [camelot.http.import :as import]
   [camelot.http.library :as library]
   [camelot.http.media :as media]
   [camelot.http.photo :as photo]
   [camelot.http.report :as report]
   [camelot.http.settings :as settings]
   [camelot.http.sighting :as sighting]
   [camelot.http.sighting-field :as sighting-field]
   [camelot.http.sighting-field-value :as sighting-field-value]
   [camelot.http.site :as site]
   [camelot.http.species-mass :as species-mass]
   [camelot.http.species-search :as species-search]
   [camelot.http.survey :as survey]
   [camelot.http.survey-file :as survey-file]
   [camelot.http.survey-site :as survey-site]
   [camelot.http.taxonomy :as taxonomy]
   [camelot.http.trap-station :as trap-station]
   [camelot.http.trap-station-session :as trap-station-session]
   [camelot.http.trap-station-session-camera :as trap-station-session-camera]))

(defroutes app-routes
  (route/resources "/" {:root "www"})
  app/routes
  camera/routes
  camera-deployment/routes
  camera-status/routes
  dataset/routes
  deployment/routes
  detector/routes
  import/routes
  library/routes
  media/routes
  photo/routes
  report/routes
  settings/routes
  sighting/routes
  sighting-field/routes
  sighting-field-value/routes
  site/routes
  species-mass/routes
  species-search/routes
  survey/routes
  survey-file/routes
  survey-site/routes
  taxonomy/routes
  trap-station/routes
  trap-station-session/routes
  trap-station-session-camera/routes)
