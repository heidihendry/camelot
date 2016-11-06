(ns camelot.report.module.builtin.reports.occupancy-matrix
  (:require
   [camelot.report.module.core :as module]
   [camelot.report.module.presence-util :as presence]
   [camelot.translation.core :as tr]
   [clojure.edn :as edn]))

(defn report-output-count
  [state {:keys [taxonomy-id start-date end-date]}]
  {:function (partial presence/generate-count taxonomy-id start-date end-date)})

(defn report-output-presence
  [state {:keys [taxonomy-id start-date end-date]}]
  {:function (partial presence/generate-presence taxonomy-id start-date end-date)})

(defn form-smith
  [state]
  {:resource {}
   :layout [[:taxonomy-id]
            [:start-date]
            [:end-date]]
   :schema {:taxonomy-id
            {:label (tr/translate (:config state) :taxonomy/title)
             :description (tr/translate (:config state) :taxonomy/report-description)
             :schema {:type :select
                      :required true
                      :get-options {:url "/taxonomy"
                                    :label :taxonomy-label
                                    :value :taxonomy-id}}}

            :start-date
            {:label (tr/translate (:config state) ::start-date)
             :description (tr/translate (:config state) ::start-date)
             :schema {:type :datetime
                      :required true}}

            :end-date
            {:label (tr/translate (:config state) ::end-date)
             :description (tr/translate (:config state) ::end-date)
             :schema {:type :datetime
                      :required true}}}})

(module/register-report
 :occupancy-matrix-count
 {:file-prefix "occupancy-matrix-species-count"
  :output report-output-count
  :title ::title-count
  :description ::description-count
  :form form-smith
  :by :all
  :for :species})

(module/register-report
 :occupancy-matrix-presence
 {:file-prefix "occupancy-matrix-presence"
  :output report-output-presence
  :title ::title-presence
  :description ::description-presence
  :form form-smith
  :by :all
  :for :species})
