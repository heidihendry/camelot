(ns camelot.report.module.builtin.reports.species-statistics
  (:require [camelot.model.taxonomy :as taxonomy]
            [camelot.report.module.core :as module]))

(defn report-configuration
  [state taxonomy-id]
  (let [spp (taxonomy/get-specific state taxonomy-id)]
    {:columns [:taxonomy-genus
               :taxonomy-species
               :trap-station-longitude
               :trap-station-latitude
               :presence-absence
               :independent-observations
               :total-nights
               :independent-observations-per-night]
     :aggregate-on [:independent-observations
                    :nights-elapsed]
     :filters [#(or (= (:taxonomy-id %) taxonomy-id)
                    (nil? (:taxonomy-id %)))]
     :transforms [#(if (= (:taxonomy-id %) taxonomy-id)
                     %
                     (select-keys % [:trap-station-longitude
                                     :trap-station-latitude
                                     :total-nights]))
                  #(if (nil? (:taxonomy-id %))
                     (assoc % :taxonomy-species
                            (:taxonomy-species spp)
                            :taxonomy-genus
                            (:taxonomy-genus spp))
                     %)]
     :order-by [:taxonomy-genus :taxonomy-species]}))

(module/register-report
 :species-statistics
 {:file-prefix "species-statistics"
  :configuration report-configuration
  :by :all
  :for :species})
