(ns camelot.report.module.builtin.reports.effort-summary
  (:require
   [camelot.report.module.core :as module]
   [camelot.translation.core :as tr]))

(defn report-output
  [state {:keys [survey-id]}]
  {:columns [:survey-name
             :site-id
             :site-name
             :site-area
             :trap-station-count
             :nights-elapsed
             :taxonomy-count
             :time-period-start
             :time-period-end]
   :aggregate-on [:trap-station-count
                  :nights-elapsed
                  :taxonomy-count
                  :time-period-start
                  :time-period-end]
   :pre-filters [#(or (not (and (integer? survey-id)
                                (pos? ^long survey-id)))
                      (= (:survey-id %) survey-id))
                 #(and (not (nil? (:time-period-start %)))
                       (not (nil? (:time-period-end %))))]
   :order-by [:site-id]})

(defn form-smith
  [state]
  {:resource {}
   :layout [[:survey-id]]
   :schema {:survey-id
            {:label (tr/translate state :survey/title)
             :description (tr/translate state :survey/report-description)
             :schema {:type :select
                      :required false
                      :get-options {:url "/surveys"
                                    :label :survey-name
                                    :value :survey-id}}}}})

(module/register-report
 :effort-summary
 {:file-prefix "effort-summary-report"
  :output report-output
  :title ::title
  :description ::description
  :form form-smith
  :by :survey})
