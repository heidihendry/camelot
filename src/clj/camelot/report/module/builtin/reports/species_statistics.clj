(ns camelot.report.module.builtin.reports.species-statistics
  (:require [camelot.model.taxonomy :as taxonomy]
            [camelot.report.module.core :as module]
            [clojure.edn :as edn]))

(defn report-output
  [state {:keys [taxonomy-id]}]
  (let [spp (taxonomy/get-specific state taxonomy-id)]
    {:columns [:taxonomy-genus
               :taxonomy-species
               :trap-station-latitude
               :trap-station-longitude
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

(def form-smith
  {:resource {}
   :layout [[:taxonomy-id]]
   :schema {:taxonomy-id
            {:label "Species"
             :description "The species to report on"
             :schema {:type :select
                      :required true
                      :get-options {:url "/taxonomy"
                                    :label :taxonomy-label
                                    :value :taxonomy-id}}}}})

(module/register-report
 :species-statistics
 {:file-prefix "species-statistics"
  :title "Species Statistics"
  :description "Sightings breakdown for a single species across all surveys."
  :output report-output
  :form form-smith
  :by :all
  :for :species})
