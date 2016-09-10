(ns camelot.report.module.builtin.reports.record-table
  (:require [camelot.report.module.core :as module]
            [clj-time.format :as tf]
            [camelot.translation.core :as tr]
            [camelot.report.sighting-independence :as indep]
            [camelot.util.config :as config]))

(defn report-output
  [state {:keys [survey-id]}]
  {:columns [:trap-station-name
             :species-name
             :media-capture-timestamp
             :media-capture-date
             :media-capture-time
             :sighting-time-delta-seconds
             :sighting-time-delta-minutes
             :sighting-time-delta-hours
             :sighting-time-delta-days
             :media-directory
             :media-filename]
   :apply-fn (partial indep/->independent-sightings)
   :transforms [#(update % :media-capture-timestamp
                         (partial tf/unparse (tf/formatters :date-time)))
                #(assoc % :media-directory (config/get-media-path))]
   :filters [#(= (:survey-id %) survey-id)]
   :order-by [:media-capture-timestamp]})

(defn form-smith
  [state]
  {:resource {}
   :layout [[:survey-id]]
   :schema {:survey-id
            {:label (tr/translate (:config state) :survey/title)
             :description (tr/translate (:config state) :survey/report-description)
             :schema {:type :select
                      :required true
                      :get-options {:url "/surveys"
                                    :label :survey-name
                                    :value :survey-id}}}}})

(defn column-titles
  [state]
  {:media-directory (tr/translate (:config state) ::media-directory)})

(module/register-report
 :record-table
 {:file-prefix "record-table"
  :output report-output
  :title ::title
  :description ::description
  :form form-smith
  :by :all
  :for :survey
  :column-title-fn column-titles})
