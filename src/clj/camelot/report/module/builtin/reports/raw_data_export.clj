(ns camelot.report.module.builtin.reports.raw-data-export
  (:require [camelot.report.module.core :as module]))

(defn report-configuration
  [state survey-id]
  {:columns [:media-filename
             :site-name
             :site-sublocation
             :trap-station-name
             :trap-station-longitude
             :trap-station-latitude
             :species-scientific-name
             :independent-observations]
   :aggregate-on [:independent-observations]
   :filters [#(= (:survey-id %) survey-id)]
   :order-by [:species-scientific-name ]})

(module/register-report
 :raw-data-export
 {:file-prefix "raw-data-export"
  :configuration report-configuration
  :by :survey
  :for :survey})
