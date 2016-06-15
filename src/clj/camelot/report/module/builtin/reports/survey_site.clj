(ns camelot.report.module.builtin.reports.survey-site
  (:require [camelot.report.module.core :as module]))

(defn report-configuration
  [state survey-site-id]
  {:columns [:species-scientific-name
             :presence-absence
             :independent-observations
             :nights-elapsed
             :independent-observations-per-night]
   :aggregate-on [:independent-observations]
   :rewrites [#(if (= (:survey-site-id %) survey-site-id)
                 %
                 (select-keys % [:species-scientific-name]))]
   :pre-transforms [#(if (= (:survey-site-id %) survey-site-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :nights-elapsed]))]
   :transforms [#(if (= (:survey-site-id %) survey-site-id)
                       %
                       (select-keys % [:species-scientific-name
                                       :nights-elapsed]))]
   :filters [#(:species-scientific-name %)]
   :order-by [:species-scientific-name ]})

(module/register-report
 :survey-site-statistics
 {:file-prefix "survey-site-statistics"
  :configuration report-configuration
  :by :species
  :for :survey-site})
