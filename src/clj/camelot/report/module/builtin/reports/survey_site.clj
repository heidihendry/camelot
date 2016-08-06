(ns camelot.report.module.builtin.reports.survey-site
  (:require [camelot.report.module.core :as module]))

(defn report-output
  [state survey-site-id]
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

(module/register-report
 :survey-site-statistics
 {:file-prefix "survey-site-statistics"
  :title "Survey Site Statistics"
  :output report-output
  :by :all
  :for :survey-site})
