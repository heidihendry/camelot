(ns camelot.report.maxent
  (:require [camelot.util.application :as app]
            [camelot.util.config :as config]
            [clojure.string :as str]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [ring.util.response :as r]
            [schema.core :as s]
            [camelot.report.core :as report]
            [clojure.edn :as edn]))

(defn report-configuration
  [survey-id]
  {:columns [:media-id
             :species-scientific-name
             :trap-station-longitude
             :trap-station-latitude]
   :aggregate-on [:independent-observations
                  :nights-elapsed]
   :filters [#(:trap-station-longitude %)
             #(:trap-station-latitude %)
             #(:species-scientific-name %)
             #(= (:survey-id %) survey-id)]
   :order-by [:species-scientific-name
              :trap-station-longitude
              :trap-station-latitude]})

(defn report
  [state survey-id sightings]
  (let [conf (report-configuration survey-id)]
    (->> sightings
         (report/report state conf)
         (report/as-rows state conf))))

(defn csv-report
  [state survey-id sightings]
  (report/exportable-report state (report-configuration survey-id) sightings))

(defn export
  "Handler for an export request."
  [survey-id]
  (let [state (app/gen-state (config/config))
        sightings (report/get-by :species)
        data (csv-report state survey-id sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"maxent.csv\""))))

(def routes
  "MaxEnt routes."
  (context "/report/maxent" []
           (GET "/:id" [id] (export (edn/read-string id)))))
