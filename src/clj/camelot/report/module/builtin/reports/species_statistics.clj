(ns camelot.report.module.builtin.reports.species-statistics
  (:require [camelot.model.species :as species]
            [camelot.report.module.core :as module]))

(defn report-configuration
  [state species-id]
  (let [spp (species/get-specific state species-id)]
    {:columns [:species-scientific-name
               :trap-station-longitude
               :trap-station-latitude
               :presence-absence
               :independent-observations
               :nights-elapsed
               :independent-observations-per-night]
     :aggregate-on [:independent-observations
                    :nights-elapsed]
     :filters [#(or (= (:species-id %) species-id)
                    (nil? (:species-id %)))]
     :transforms [#(if (= (:species-id %) species-id)
                     %
                     (select-keys % [:trap-station-longitude
                                     :trap-station-latitude
                                     :nights-elapsed]))
                  #(if (nil? (:species-id %))
                     (assoc % :species-scientific-name
                            (:species-scientific-name spp))
                     %)]
     :order-by [:species-scientific-name]}))

(module/register-report
 :species-statistics
 {:file-prefix "species-statistics"
  :configuration report-configuration
  :by :species
  :for :species})
