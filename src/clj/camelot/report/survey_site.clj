(ns camelot.report.survey-site
  (:require [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [camelot.application :as app]
            [camelot.util.config :as config]
            [ring.util.response :as r]
            [camelot.report-builder.core :as report]
            [clojure.edn :as edn]))

(defn report-configuration
  [survey-site-id]
  {:columns [:species-scientific-name
             :presence-absence
             :independent-observations
             :nights-elapsed
             :independent-observations-per-night]
   :aggregate-on [:independent-observations
                  :nights-elapsed]
   :pre-transforms [#(if (= (:survey-site-id %) survey-site-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :nights-elapsed]))]
   :transforms [#(if (= (:survey-site-id %) survey-site-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :nights-elapsed]))]
   :filters [#(:species-scientific-name %)]
   :order-by [:species-scientific-name ]})

(defn report
  [state survey-site-id sightings]
  (let [conf (report-configuration survey-site-id)]
    (->> sightings
         (report/report state conf)
         (report/as-rows state conf))))

(defn csv-report
  [state survey-site-id sightings]
  (report/exportable-report
   state
   (report-configuration survey-site-id) sightings))

(defn export
  [survey-site-id]
  (let [state (app/gen-state (config/config))
        sightings (report/get-by :species)
        data (csv-report state survey-site-id sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"survey-site-statistics.csv\""))))

(def routes
  "Species summary report routes."
  (context "/report/survey-site-statistics" []
           (GET "/:id" [id] (export (edn/read-string id)))))
