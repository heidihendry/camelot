(ns camelot.report.module.builtin.reports.camera-traps
  (:require [camelot.report.module.core :as module]
            [clj-time.format :as tf]
            [camelot.translation.core :as tr]))

(def date-format (tf/formatter-local "yyyy-MM-dd"))

(defn report-output
  [state {:keys [survey-id]}]
  {:columns [:trap-station-name
             :trap-station-session-camera-id
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
  {:trap-station-name "Station"
   :trap-station-session-camera-id "Camera"
   :trap-station-latitude "gps_y"
   :trap-station-longitude "gps_x"
   :trap-station-session-start-date "Setup_date"
   :trap-station-session-end-date "Retrieval_date"})

(module/register-report
 :camera-traps
 {:file-prefix "camera-traps"
  :output report-output
  :title ::title
  :description ::description
  :form form-smith
  :by :all
  :for :survey
  :column-title-fn column-titles})
