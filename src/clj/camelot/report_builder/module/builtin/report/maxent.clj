(ns camelot.report-builder.module.builtin.report.maxent
  (:require [camelot.report-builder.module.core :as module]))

(defn report-configuration
  [state survey-id]
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

(module/register-report
 :maxent
 {:file-prefix "maxent"
  :configuration report-configuration
  :by :species
  :for :survey})
