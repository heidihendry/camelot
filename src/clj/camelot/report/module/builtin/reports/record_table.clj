(ns camelot.report.module.builtin.reports.record-table
  (:require
   [camelot.report.module.core :as module]
   [clj-time.core :as t]
   [clj-time.format :as tf]
   [camelot.translation.core :as tr]
   [camelot.report.sighting-independence :as indep]
   [camelot.util.config :as config]))

(defn report-output
  [state {:keys [survey-id]}]
  {:columns [:trap-station-name
             :trap-station-session-camera-id
             :camera-name
             :species-name
             :trap-camera-pair
             :media-capture-timestamp
             :media-capture-date
             :media-capture-time
             :sighting-time-delta-seconds
             :sighting-time-delta-minutes
             :sighting-time-delta-hours
             :sighting-time-delta-days
             :media-directory
             :media-full-filename]
   :apply-fn indep/->independent-sightings
   :transforms [#(update % :media-capture-timestamp
                         (partial tf/unparse (tf/formatters :mysql)))
                #(assoc % :media-directory (get-in state [:config :path :media]))
                #(assoc % :trap-camera-pair (format "%s_%s"
                                                    (:trap-station-name %)
                                                    (:trap-station-session-camera-id %)))
                #(assoc % :media-full-filename (if (:media-filename %)
                                                 (str (:media-filename %) "."
                                                      (:media-format %))
                                                 nil))]
   :filters [#(= (:survey-id %) survey-id)]
   :order-by [:media-capture-timestamp]})

(defn form-smith
  [state]
  {:resource {}
   :layout [[:survey-id]]
   :schema {:survey-id
            {:label (tr/translate state :survey/title)
             :description (tr/translate state :survey/report-description)
             :schema {:type :select
                      :required true
                      :get-options {:url "/surveys"
                                    :label :survey-name
                                    :value :survey-id}}}}})

(defn column-titles
  [state]
  {:trap-station-name "Station"
   :camera-name "CameraName"
   :trap-station-session-camera-id "Camera"
   :species-name "Species"
   :trap-camera-pair "TrapAndCamera"
   :media-capture-timestamp "DateTimeOriginal"
   :media-capture-date "Date"
   :media-capture-time "Time"
   :sighting-time-delta-seconds "delta.time.secs"
   :sighting-time-delta-minutes "delta.time.mins"
   :sighting-time-delta-hours "delta.time.hours"
   :sighting-time-delta-days "delta.time.days"
   :media-directory "Directory"
   :media-full-filename "FileName"})

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
