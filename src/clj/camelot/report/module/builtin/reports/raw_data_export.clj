(ns camelot.report.module.builtin.reports.raw-data-export
  (:require [camelot.report.module.core :as module]
            [clj-time.format :as tf]))

(def timestamp-formatter (tf/formatter "yyyy-MM-dd hh:mm:ss"))

(defn report-output
  [state {:keys [survey-id]}]
  {:columns [:media-filename
             :media-capture-timestamp
             :site-name
             :site-sublocation
             :trap-station-name
             :trap-station-latitude
             :trap-station-longitude
             :taxonomy-genus
             :taxonomy-species
             :sighting-quantity]
   :filters [#(= (:survey-id %) survey-id)
             #(not (nil? (:media-id %)))]
   :transforms [#(update % :media-capture-timestamp
                         (partial tf/unparse timestamp-formatter))]
   :order-by [:taxonomy-genus :taxonomy-species]})

(def form-smith
  {:resource {}
   :layout [[:survey-id]]
   :schema {:survey-id
            {:label "Survey"
             :description "The survey to report on"
             :schema {:type :select
                      :required true
                      :get-options {:url "/surveys"
                                    :label :survey-name
                                    :value :survey-id}}}}})

(module/register-report
 :raw-data-export
 {:file-prefix "raw-data-export"
  :title "Raw Data Export"
  :description "Details about each uploaded capture."
  :output report-output
  :form form-smith
  :by :all
  :for :survey})
