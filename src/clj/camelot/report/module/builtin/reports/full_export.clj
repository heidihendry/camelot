(ns camelot.report.module.builtin.reports.full-export
  (:require
   [camelot.report.module.core :as module]
   [camelot.model.media :as media]
   [camelot.util.bulk-import :as bulk-import]
   [clj-time.format :as tf]
   [camelot.translation.core :as tr])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(def timestamp-formatter (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(def timestamp-columns
  [:media-capture-timestamp
   :media-updated
   :media-created
   :sighting-updated
   :sighting-created
   :taxonomy-created
   :taxonomy-updated
   :trap-station-session-start-date
   :trap-station-session-end-date])

(defn- format-timestamps
  [record]
  (reduce #(update %1 %2 (partial tf/unparse timestamp-formatter))
          record timestamp-columns))

(defn report-output
  [state {:keys []}]
  {:columns [:all]
   :filters [#(not (nil? (:media-id %)))]
   :transforms [#(assoc % :absolute-path
                        (media/path-to-media state :original %))
                format-timestamps]
   :options {:leave-blank-fields-empty true}})

(defn form-smith
  [state]
  {:resource {}
   :layout []})

(defn column-titles
  [state]
  (assoc bulk-import/default-column-mappings :absolute-path "Absolute Path"))

(module/register-report
 :full-export
 {:file-prefix "full-export"
  :title ::title
  :description ::description
  :output report-output
  :form form-smith
  :by :all
  :column-title-fn column-titles})
