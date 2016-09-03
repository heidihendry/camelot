(ns camelot.report.module.builtin.reports.camera-traps
  (:require [camelot.report.module.core :as module]
            [clj-time.format :as tf]))

(def date-format (tf/formatter-local "yyyy-MM-dd"))

(defn report-output
  [state {:keys [survey-id]}]
  {:columns [:trap-station-session-camera-id
             :trap-station-latitude
             :trap-station-longitude
             :trap-station-session-start-date
             :trap-station-session-end-date]
   :aggregate-on []
   :rewrites [#(update % :trap-station-session-start-date
                       (fn [x] (tf/unparse date-format x)))
              #(update % :trap-station-session-end-date
                       (fn [x] (tf/unparse date-format x)))]
   :filters [#(= (:survey-id %) survey-id)]
   :order-by [:trap-station-id :trap-station-session-id]})

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
 :effort-summary
 {:file-prefix "camera-traps"
  :output report-output
  :title "Camera Trap Export"
  :description "A CamtrapR-compatible export of camera trap details. Set byCamera to TRUE when importing into CamtrapR."
  :form form-smith
  :by :all
  :for :survey})
