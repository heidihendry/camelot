(ns camelot.report.module.builtin.reports.full-export
  (:require
   [camelot.report.module.core :as module]
   [camelot.import.template :as template]
   [clj-time.format :as tf]
   [camelot.translation.core :as tr])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(defn report-output
  [state {:keys []}]
  {:columns [:all]
   :filters [#(not (nil? (:media-id %)))]
   :transforms [#(assoc % :absolute-path
                        (str (get-in state [:config :path :media])
                             SystemUtils/FILE_SEPARATOR
                             (:media-filename %)))]
   :options {:leave-blank-fields-empty true}})

(defn form-smith
  [state]
  {:resource {}
   :layout []})

(defn column-titles
  [state]
  template/default-column-mappings)

(module/register-report
 :full-export
 {:file-prefix "full-export"
  :title ::title
  :description ::description
  :output report-output
  :form form-smith
  :by :all
  :for :survey
  :column-title-fn column-titles})
