(ns camelot.report.module.builtin.reports.survey-site
  (:require [camelot.report.module.core :as module]
            [camelot.translation.core :as tr]))

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

(defn form-smith
  [state]
  {:resource {}
   :layout [[:survey-site-id]]
   :schema {:survey-site-id
            {:label (tr/translate (:config state) :survey/title)
             :description (tr/translate (:config state) :survey/report-description)
             :schema {:type :select
                      :required true
                      :get-options {:url "/survey-sites"
                                    :label :site-name
                                    :value :survey-site-id}}}}})

(module/register-report
 :survey-site-statistics
 {:file-prefix "survey-site-statistics"
  :title ::title
  :description ::description
  :output report-output
  :form form-smith
  :by :all
  :for :survey-site})
