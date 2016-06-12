(ns camelot.report.summary-statistics
  (:require [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [camelot.report-builder.core :as report]
            [camelot.application :as app]
            [camelot.util.config :as config]
            [ring.util.response :as r]))

(defn report-configuration
  [survey-id]
  {:columns [:species-scientific-name
             :trap-station-count
             :media-count
             :independent-observations
             :nights-elapsed
             :independent-observations-per-night]
   :aggregate-on [:media-count
                  :independent-observations
                  :nights-elapsed
                  :trap-station-count]
   :filters [#(not (nil? (:species-scientific-name %)))
             #(= (:survey-id %) survey-id)]
   :order-by [:species-scientific-name ]})

(defn report
  [state survey-id sightings]
  (let [config (report-configuration survey-id)]
    (->> sightings
         (report/report state config)
         (report/as-rows state config))))

(defn csv-report
  [state survey-id sightings]
  (report/exportable-report
   state
   (report-configuration survey-id) sightings))

(defn export
  [survey-id]
  (let [state (app/gen-state (config/config))
        sightings (report/get-by :species)
        data (csv-report state survey-id sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"summary-statistics-report.csv\""))))

(def routes
  "Species summary report routes."
  (context "/report/summary-statistics" []
           (GET "/:id" [id] (export id))))
