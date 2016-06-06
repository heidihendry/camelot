(ns camelot.report.raw-data-export
  (:require [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [camelot.util.application :as app]
            [camelot.util.config :as config]
            [ring.util.response :as r]
            [clojure.edn :as edn]
            [camelot.report.core :as report]))

(defn report-configuration
  [survey-id]
  {:columns [:media-filename
             :site-name
             :site-sublocation
             :trap-station-name
             :trap-station-longitude
             :trap-station-latitude
             :species-scientific-name
             :independent-observations]
   :aggregate-on [:independent-observations]
   :filters [#(= (:survey-id %) survey-id)]
   :order-by [:species-scientific-name ]})

(defn report
  [state survey-id sightings]
  (let [conf (report-configuration survey-id)]
    (->> sightings
         (report/report state conf)
         (report/as-rows state conf))))

(defn csv-report
  [state survey-id sightings]
  (report/exportable-report
   state
   (report-configuration survey-id) sightings))

(defn export
  [survey-id]
  (let [state (app/gen-state (config/config))
        sightings (report/get-by state :survey)
        data (csv-report state survey-id sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"raw-data-export.csv\""))))

(def routes
  "Survey summary report routes."
  (context "/report/raw-data-export" []
           (GET "/:id" [id] (export (edn/read-string id)))))
