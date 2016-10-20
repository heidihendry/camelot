(ns camelot.report.module.builtin.reports.full-export
  (:require [camelot.report.module.core :as module]
            [clj-time.format :as tf]
            [camelot.translation.core :as tr]))

(def timestamp-formatter (tf/formatter "yyyy-MM-dd hh:mm:ss"))

(defn report-output
  [state {:keys []}]
  {:columns [:all]})

(defn form-smith
  [state]
  {:resource {}
   :layout []})

(module/register-report
 :full-export
 {:file-prefix "full-export"
  :title ::title
  :description ::description
  :output report-output
  :form form-smith
  :by :all
  :for :survey})
