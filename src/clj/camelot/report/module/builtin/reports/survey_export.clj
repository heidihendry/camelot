(ns camelot.report.module.builtin.reports.survey-export
  (:require
   [camelot.report.module.core :as module]
   [camelot.import.template :as template]
   [clj-time.format :as tf]
   [camelot.translation.core :as tr])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(defn report-output
  [state {:keys [survey-id]}]
  {:columns [:all]
   :filters [#(not (nil? (:media-id %)))
             #(= (:survey-id %) survey-id)]
   :transforms [#(assoc % :absolute-path
                        (str (get-in state [:config :path :media])
                             SystemUtils/FILE_SEPARATOR
                             (:media-filename %)))]
   :options {:leave-blank-fields-empty true}})

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
  template/default-column-mappings)

(module/register-report
 :survey-export
 {:file-prefix "survey-export"
  :title ::title
  :description ::description
  :output report-output
  :form form-smith
  :by :all
  :for :survey
  :column-title-fn column-titles})
