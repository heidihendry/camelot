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
             :taxonomy-genus
             :taxonomy-species
             :sighting-quantity]
   :filters [#(= (:survey-id %) survey-id)]
   :order-by [:taxonomy-genus :taxonomy-species]})

(module/register-report
 :raw-data-export
 {:file-prefix "raw-data-export"
  :configuration report-configuration
  :by :all
  :for :survey})
