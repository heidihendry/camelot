(ns camelot.report.module.builtin.reports.trap-station
  (:require
   [camelot.model.trap-station :as trap-station]
   [camelot.report.module.core :as module]
   [camelot.translation.core :as tr]))

(defn include?
  [trap-station-id record]
  (= (:orig-ts-id record) trap-station-id))

(defn report-output
  [state {:keys [trap-station-id]}]
  (let [ts (trap-station/get-specific state trap-station-id)]
    {:columns [:trap-station-id
               :trap-station-name
               :taxonomy-genus
               :taxonomy-species
               :presence-absence
               :independent-observations
               :total-nights
               :independent-observations-per-night]
     :aggregate-on [:independent-observations]
     :rewrites [#(assoc % :orig-ts-id (:trap-station-id %))
                #(if (include? trap-station-id %)
                   %
                   (select-keys % [:taxonomy-species
                                   :taxonomy-genus]))
                #(if (nil? (:trap-station-id %))
                   (assoc % :trap-station-id (:trap-station-id ts)
                          :trap-station-name (:trap-station-name ts))
                   %)]
     :transforms [#(if (include? trap-station-id %)
                     %
                     (select-keys % [:taxonomy-species
                                     :taxonomy-genus
                                     :trap-station-id
                                     :trap-station-name
                                     :total-nights]))]
     :filters [#(:taxonomy-species %)]
     :order-by [:trap-station-id :taxonomy-genus :taxonomy-species]}))

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
