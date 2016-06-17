(ns camelot.report.module.builtin.reports.trap-station
  (:require [camelot.report.module.core :as module]))

(defn report-configuration
  [state trap-station-id]
  {:columns [:species-scientific-name
             :presence-absence
             :independent-observations
             :total-nights
             :independent-observations-per-night]
   :aggregate-on [:independent-observations]
   :rewrites [#(if (= (:trap-station-id %) trap-station-id)
                 %
                 (select-keys % [:species-scientific-name]))]
   :pre-transforms [#(if (= (:trap-station-id %) trap-station-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :total-nights]))]
   :transforms [#(if (= (:trap-station-id %) trap-station-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :total-nights]))]
   :filters [#(:species-scientific-name %)]
   :order-by [:species-scientific-name ]})

(module/register-report
 :trap-station-statistics
 {:file-prefix "trap-station-statistics"
  :configuration report-configuration
  :by :all
  :for :trap-station})
