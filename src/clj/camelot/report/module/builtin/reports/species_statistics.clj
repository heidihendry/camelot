(ns camelot.report.module.builtin.reports.species-statistics
  (:require [camelot.model.taxonomy :as taxonomy]
            [camelot.report.module.core :as module]
            [clojure.edn :as edn]
            [camelot.translation.core :as tr]))

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
               :independent-observations-per-night
               :taxonomy-common-name
               :taxonomy-family
               :taxonomy-order
               :taxonomy-class
               :species-mass-id]
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

(defn form-smith
  [state]
  {:resource {}
   :layout [[:taxonomy-id]]
   :schema {:taxonomy-id
            {:label (tr/translate (:config state) :taxonomy/title)
             :description (tr/translate (:config state) :taxonomy/report-description)
             :schema {:type :select
                      :required true
                      :get-options {:url "/taxonomy"
                                    :label :taxonomy-label
                                    :value :taxonomy-id}}}}})

(module/register-report
 :species-statistics
 {:file-prefix "species-statistics"
  :title ::title
  :description ::description
  :output report-output
  :form form-smith
  :by :all
  :for :species})
