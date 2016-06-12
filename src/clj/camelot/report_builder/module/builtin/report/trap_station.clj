(ns camelot.report-builder.module.builtin.report.trap-station
  (:require [camelot.report-builder.module.core :as module]))

(defn report-configuration
  [state trap-station-id]
  {:columns [:species-scientific-name
             :presence-absence
             :independent-observations
             :nights-elapsed
             :independent-observations-per-night]
   :aggregate-on [:independent-observations
                  :nights-elapsed]
   :pre-transforms [#(if (= (:trap-station-id %) trap-station-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :nights-elapsed]))]
   :transforms [#(if (= (:trap-station-id %) trap-station-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :nights-elapsed]))]
   :filters [#(:species-scientific-name %)]
   :order-by [:species-scientific-name ]})

(module/register-report
 :trap-station-statistics
 {:file-prefix "trap-station-statistics"
  :configuration report-configuration
  :by :species
  :for :trap-station})
