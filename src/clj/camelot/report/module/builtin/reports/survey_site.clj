(ns camelot.report.module.builtin.reports.survey-site
  (:require
   [camelot.model.survey-site :as survey-site]
   [camelot.report.module.core :as module]
   [camelot.translation.core :as tr]))

(defn report-output
  [state {:keys [survey-site-id]}]
  (let [ss (survey-site/get-specific state survey-site-id)]
    {:columns [:survey-name
               :site-name
               :taxonomy-genus
               :taxonomy-species
               :presence-absence
               :independent-observations
               :total-nights
               :independent-observations-per-night]
     :aggregate-on [:independent-observations]
     :rewrites [#(if (= (:survey-site-id %) survey-site-id)
                   %
                   (select-keys % [:taxonomy-species
                                   :taxonomy-genus]))
                #(assoc % :site-name (:site-name ss)
                        :survey-name (:survey-name ss))]
     :transforms [#(if (= (:survey-site-id %) survey-site-id)
                     %
                     (select-keys % [:taxonomy-species
                                     :taxonomy-genus
                                     :survey-name
                                     :site-name
                                     :total-nights]))]
     :filters [#(:taxonomy-species %)]
     :order-by [:taxonomy-genus :taxonomy-species]}))

(defn form-smith
  [state]
  {:resource {}
   :layout [[:survey-site-id]]
   :schema {:survey-site-id
            {:label (tr/translate state :survey-site/title)
             :description (tr/translate state :survey-site/report-description)
             :schema {:type :select
                      :required true
                      :get-options {:url "/survey-sites"
                                    :label :survey-site-label
                                    :value :survey-site-id}}}}})

(module/register-report
 :survey-site-statistics
 {:file-prefix "survey-site-statistics"
  :title ::title
  :description ::description
  :output report-output
  :form form-smith
  :by :all})
