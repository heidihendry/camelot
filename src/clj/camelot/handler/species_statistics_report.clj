(ns camelot.handler.species-statistics-report
  (:require [camelot.db :as db]
            [yesql.core :as sql]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [camelot.util.application :as app]
            [camelot.util.config :as config]
            [camelot.util.report :as report-util]
            [ring.util.response :as r]
            [clojure.edn :as edn]
            [camelot.report-builder :as report-builder]))

(defn report-configuration
  [species-id]
  {:columns [:species-scientific-name
             :trap-station-longitude
             :trap-station-latitude
             :presence-absence
             :independent-observations
             :nights-elapsed
             :independent-observations-per-night]
   :aggregate-on [:independent-observations
                  :nights-elapsed]
   :filters [#(= (:species-id %) species-id)]
   :order-by [:species-scientific-name ]})

(defn report
  [state species-id sightings]
  (let [conf (report-configuration species-id)]
    (->> sightings
         (report-builder/report state conf)
         (report-builder/as-rows state conf))))

(defn csv-report
  [state species-id sightings]
  (report-builder/exportable-report
   state
   (report-configuration species-id) sightings))

(defn export
  [species-id]
  (let [state (app/gen-state (config/config))
        sightings (report-builder/get-data-by state :species)
        data (csv-report state species-id sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"species-statistics.csv\""))))

(def routes
  "Species summary report routes."
  (context "/report/species-statistics" []
           (GET "/:id" [id] (export (edn/read-string id)))))
