(ns camelot.report.module.builtin.reports.survey-site
  (:require [camelot.report.module.core :as module]))

(defn report-output
  [state {:keys [survey-site-id]}]
  {:columns [:taxonomy-genus
             :taxonomy-species
             :presence-absence
             :independent-observations
             :total-nights
             :independent-observations-per-night]
   :aggregate-on [:independent-observations]
   :rewrites [#(if (= (:survey-site-id %) survey-site-id)
                 %
                 (select-keys % [:taxonomy-species
                                 :taxonomy-genus]))]
   :pre-transforms [#(if (= (:survey-site-id %) survey-site-id)
                       %
                       (select-keys % [:taxonomy-species
                                       :taxonomy-genus
                                       :total-nights]))]
   :transforms [#(if (= (:survey-site-id %) survey-site-id)
                       %
                       (select-keys % [:taxonomy-species
                                       :taxonomy-genus
                                       :total-nights]))]
   :filters [#(:taxonomy-species %)]
   :order-by [:taxonomy-genus :taxonomy-species]})

(def form-smith
  {:resource {}
   :layout [[:survey-site-id]]
   :schema {:survey-site-id
            {:label "Survey Site"
             :description "The survey site to report on"
             :schema {:type :select
                      :required true
                      :get-options {:url "/survey-sites"
                                    :label :site-name
                                    :value :survey-site-id}}}}})

(module/register-report
 :survey-site-statistics
 {:file-prefix "survey-site-statistics"
  :title "Survey Site Statistics"
  :description "Describe me"
  :output report-output
  :form form-smith
  :by :all
  :for :survey-site})
