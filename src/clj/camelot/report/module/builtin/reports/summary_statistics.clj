(ns camelot.report.module.builtin.reports.summary-statistics
  (:require
   [camelot.report.module.core :as module]
   [camelot.translation.core :as tr]))

(defn report-output
  [state {:keys [survey-id]}]
  {:columns [:taxonomy-genus
             :taxonomy-species
             :trap-station-count
             :media-count
             :independent-observations
             :percent-nocturnal
             :total-nights
             :independent-observations-per-night]
   :aggregate-on [:media-count
                  :percent-nocturnal
                  :independent-observations
                  :trap-station-count]
   :rewrites [#(if (= (:survey-id %) survey-id)
                 %
                 (select-keys % [:taxonomy-species :taxonomy-genus]))]
   :filters [#(not (nil? (:taxonomy-species %)))]
   :order-by [:taxonomy-genus :taxonomy-species]})

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

(module/register-report
 :summary-statistics
 {:file-prefix "summary-statistics-report"
  :title ::title
  :description ::description
  :output report-output
  :form form-smith
  :by :all})
