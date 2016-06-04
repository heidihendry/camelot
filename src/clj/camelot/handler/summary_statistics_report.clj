(ns camelot.handler.summary-statistics-report
  (:require [camelot.db :as db]
            [yesql.core :as sql]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [camelot.report-builder :as report-builder]
            [camelot.util.application :as app]
            [camelot.util.config :as config]
            [camelot.util.report :as report-util]
            [ring.util.response :as r]))

(sql/defqueries "sql/reports.sql" {:connection db/spec})

(def report-configuration
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
   :filters [#(not (nil? (:species-scientific-name %)))]
   :order-by [:species-scientific-name ]})

(defn report
  [state sightings]
  (->> sightings
       (report-builder/report state report-configuration)
       (report-builder/as-rows state report-configuration)))

(defn csv-report
  [state sightings]
  (report-builder/exportable-report
   state
   report-configuration sightings))

(defn export
  [survey-id]
  (let [state (app/gen-state (config/config))
        sightings (report-builder/get-data-by state :species)
        data (csv-report state sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"summary-statistics-report.csv\""))))

(def routes
  "Species summary report routes."
  (context "/report/summary-statistics" []
           (GET "/:id" [id] (export id))))
