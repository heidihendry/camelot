(ns camelot.report.module.builtin.reports.effort-summary
  (:require [camelot.report.module.core :as module]))

(defn report-output
  [state survey-id]
  {:columns [:site-id
             :site-area
             :trap-station-count
             :nights-elapsed
             :taxonomy-count
             :time-period-start
             :time-period-end]
   :aggregate-on [:trap-station-count
                  :nights-elapsed
                  :taxonomy-count
                  :time-period-start
                  :time-period-end]
   :filters [#(= (:survey-id %) survey-id)]
   :order-by [:site-id]})


(module/register-report
 :effort-summary
 {:file-prefix "effort-summary-report"
  :output report-output
  :title "Effort Summary Report"
  :by :all
  :for :survey})
