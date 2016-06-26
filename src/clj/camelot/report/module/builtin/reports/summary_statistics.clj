(ns camelot.report.module.builtin.reports.summary-statistics
  (:require [camelot.report.module.core :as module]))

(defn report-configuration
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
  :configuration report-configuration
  :by :all
  :for :survey})
