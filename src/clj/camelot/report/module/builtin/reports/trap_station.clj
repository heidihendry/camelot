(ns camelot.report.module.builtin.reports.trap-station
  (:require
   [camelot.report.module.core :as module]
   [camelot.translation.core :as tr]))

(defn report-output
  [state {:keys [trap-station-id]}]
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

(defn form-smith
  [state]
  {:resource {}
   :layout [[:trap-station-id]]
   :schema {:trap-station-id
            {:label (tr/translate state :trap-station/trap-station-name.label)
             :description (tr/translate state :trap-station/report-description)
             :schema {:type :select
                      :required true
                      :get-options {:url "/trap-stations"
                                    :label :trap-station-name
                                    :value :trap-station-id}}}}})

(module/register-report
 :trap-station-statistics
 {:file-prefix "trap-station-statistics"
  :title ::title
  :description ::description
  :output report-output
  :form form-smith
  :by :survey})
