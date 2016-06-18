(ns camelot.report.module.builtin.reports.maxent
  (:require [camelot.report.module.core :as module]))

(defn report-configuration
  [state survey-id]
  {:columns [:media-id
             :taxonomy-genus
             :taxonomy-species
             :trap-station-longitude
             :trap-station-latitude]
   :aggregate-on [:independent-observations]
   :filters [#(:trap-station-longitude %)
             #(:trap-station-latitude %)
             #(:taxonomy-species %)
             #(= (:survey-id %) survey-id)]
   :order-by [:taxonomy-species
              :trap-station-longitude
              :trap-station-latitude]})

(module/register-report
 :maxent
 {:file-prefix "maxent"
  :configuration report-configuration
  :by :species
  :for :survey})
