(ns camelot.report.module.builtin.reports.effort-summary
  (:require [camelot.report.module.core :as module]))

(defn report-configuration
  [state survey-id]
  {:columns [:site-id
             :site-area
             :trap-station-count
             :nights-elapsed
             :species-count
             :time-period-start
             :time-period-end]
   :aggregate-on [:trap-station-count
                  :nights-elapsed
                  :species-count
                  :time-period-start
                  :time-period-end]
   :filters [#(= (:survey-id %) survey-id)]
   :order-by [:site-id]})

(module/register-report
 :effort-summary
 {:file-prefix "effort-summary-report"
  :configuration report-configuration
  :by :all
  :for :survey})
