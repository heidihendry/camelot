(ns camelot.report.module.builtin.reports.summary-statistics
  (:require [camelot.report.module.core :as module]))

(defn report-output
  [state survey-id]
  {:columns [:taxonomy-genus
             :taxonomy-species
             :trap-station-count
             :media-count
             :independent-observations
             :percent-nocturnal
             :total-nights
             :independent-observations-per-night]
   :aggregate-on [:media-count
                  :percent-nocturnal
                  :independent-observations
                  :trap-station-count]
   :rewrites [#(if (= (:survey-id %) survey-id)
                 %
                 (select-keys % [:taxonomy-species :taxonomy-genus]))]
   :filters [#(not (nil? (:taxonomy-species %)))]
   :order-by [:taxonomy-genus :taxonomy-species]})

(module/register-report
 :summary-statistics
 {:file-prefix "summary-statistics-report"
  :title "Summary Statistics Report"
  :output report-output
  :by :all
  :for :survey})
