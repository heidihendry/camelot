(ns camelot.report.module.builtin.reports.effort-summary
  (:require [camelot.report.module.core :as module]))

(defn report-output
  [state {:keys [survey-id]}]
  {:columns [:site-id
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
   :filters [#(= (:survey-id %) survey-id)]
   :order-by [:site-id]})

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
 {:file-prefix "effort-summary-report"
  :output report-output
  :title "Effort Summary"
  :description "A breakdown of sites in a survey and their trap stations."
  :form form-smith
  :by :all
  :for :survey})
