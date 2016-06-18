(ns camelot.report.module.builtin.reports.trap-station
  (:require [camelot.report.module.core :as module]))

(defn report-configuration
  [state trap-station-id]
  {:columns [:taxonomy-genus
             :taxonomy-species
             :presence-absence
             :independent-observations
             :total-nights
             :independent-observations-per-night]
   :aggregate-on [:independent-observations]
   :rewrites [#(if (= (:trap-station-id %) trap-station-id)
                 %
                 (select-keys % [:taxonomy-species
                                 :taxonomy-genus]))]
   :pre-transforms [#(if (= (:trap-station-id %) trap-station-id)
                       %
                       (select-keys % [:taxonomy-species
                                       :taxonomy-genus
                                       :total-nights]))]
   :transforms [#(if (= (:trap-station-id %) trap-station-id)
                       %
                       (select-keys % [:taxonomy-species
                                       :taxonomy-genus
                                       :total-nights]))]
   :filters [#(:taxonomy-species %)]
   :order-by [:taxonomy-genus :taxonomy-species]})

(module/register-report
 :trap-station-statistics
 {:file-prefix "trap-station-statistics"
  :configuration report-configuration
  :by :all
  :for :trap-station})
