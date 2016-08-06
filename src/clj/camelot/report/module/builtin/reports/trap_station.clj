(ns camelot.report.module.builtin.reports.trap-station
  (:require [camelot.report.module.core :as module]))

(defn report-output
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

(def form-smith
  {:layout [[:trap-station-id]]
   :schema {:trap-station-id {:type :select
                              :required true
                              :options {:url "/trap-stations"
                                        :label :trap-station-name
                                        :value :trap-station-id}}}})

(module/register-report
 :trap-station-statistics
 {:file-prefix "trap-station-statistics"
  :title "Trap Station Statistics"
  :description "Describe the observations at a given trap station and the time elapsed gathering those observations."
  :output report-output
  :form form-smith
  :by :all
  :for :trap-station})
