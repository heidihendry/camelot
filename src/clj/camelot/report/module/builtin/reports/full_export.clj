(ns camelot.report.module.builtin.reports.full-export
  (:require
   [camelot.report.module.core :as module]
   [camelot.model.media :as media]
   [camelot.util.bulk-import :as bulk-import]
   [clj-time.format :as tf]
   [camelot.translation.core :as tr])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(defn report-output
  [state {:keys []}]
  {:columns [:all]
   :filters [#(not (nil? (:media-id %)))]
   :transforms [#(assoc % :absolute-path
                        (media/path-to-media state :original %))]
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
