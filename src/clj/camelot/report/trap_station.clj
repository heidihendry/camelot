(ns camelot.report.trap-station
  (:require [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [camelot.util.application :as app]
            [camelot.util.config :as config]
            [ring.util.response :as r]
            [clojure.edn :as edn]
            [camelot.report.core :as report]))

(defn report-configuration
  [trap-station-id]
  {:columns [:species-scientific-name
             :presence-absence
             :independent-observations
             :nights-elapsed
             :independent-observations-per-night]
   :aggregate-on [:independent-observations
                  :nights-elapsed]
   :pre-transforms [#(if (= (:trap-station-id %) trap-station-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :nights-elapsed]))]
   :transforms [#(if (= (:trap-station-id %) trap-station-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :nights-elapsed]))]
   :filters [#(:species-scientific-name %)]
   :order-by [:species-scientific-name ]})

(defn report
  [state trap-station-id sightings]
  (let [conf (report-configuration trap-station-id)]
    (->> sightings
         (report/report state conf)
         (report/as-rows state conf))))

(defn csv-report
  [state trap-station-id sightings]
  (report/exportable-report
   state
   (report-configuration trap-station-id) sightings))

(defn export
  [trap-station-id]
  (let [state (app/gen-state (config/config))
        sightings (report/get-by :species)
        data (csv-report state trap-station-id sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"trap-station-statistics.csv\""))))

(def routes
  "Species summary report routes."
  (context "/report/trap-station-statistics" []
           (GET "/:id" [id] (export (edn/read-string id)))))
