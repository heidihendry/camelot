(ns camelot.report.module.builtin.reports.summary-statistics
  (:require [camelot.report.module.core :as module]))

(defn report-configuration
  [state survey-id]
  {:columns [:species-scientific-name
             :trap-station-count
             :media-count
             :independent-observations
             :total-nights
             :independent-observations-per-night]
   :aggregate-on [:media-count
                  :independent-observations
                  :trap-station-count]
   :rewrites [#(if (= (:survey-id %) survey-id)
                 %
                 (select-keys % [:species-scientific-name]))]
   :filters [#(not (nil? (:species-scientific-name %)))]
   :order-by [:species-scientific-name ]})

(module/register-report
 :summary-statistics
 {:file-prefix "summary-statistics-report"
  :configuration report-configuration
  :by :all
  :for :survey})
