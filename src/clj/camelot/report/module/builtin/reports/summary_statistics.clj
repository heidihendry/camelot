(ns camelot.report.module.builtin.reports.summary-statistics
  (:require [camelot.report.module.core :as module]))

(defn report-configuration
  [state survey-id]
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

(module/register-report
 :summary-statistics
 {:file-prefix "summary-statistics-report"
  :configuration report-configuration
  :by :species
  :for :survey})
