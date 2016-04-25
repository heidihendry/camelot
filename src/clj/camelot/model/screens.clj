(ns camelot.model.screens
  (:require [camelot.smithy.core :refer [defsmith] :as smithy]))

(defn translate-fn
  [state]
  (fn [resource lookup]
    ((:translate state) (keyword (format "%s/%s" (name resource) (subs (str lookup) 1))))))

(def smiths (atom {}))

(defn smith
  [state]
  (smithy/build-smiths smiths (translate-fn state) state))

;;(settings-screen (settings/gen-state settings/default-config))

(defsmith site smiths
  [state]
  {:resource {:type :site
              :title ((:translate state) :site/title)
              :endpoint "/site"}
   :sidebar {:resource {:endpoint "/site"
                        :title ((:translate state) :site/sidebar-title)
                        :type :site
                        :id :site_id
                        :label :site_name}}
   :layout [[:site-name]
            [:site-sublocation]
            [:site-city]
            [:site-state-province]
            [:site-country]
            [:site-notes]]
   :schema {:site-name {:type :text}
            :site-sublocation {:type :text}
            :site-city {:type :text}
            :site-state-province {:type :text}
            :site-country {:type :text}
            ;; TODO make textarea
            :site-notes {:type :text}}
   :states {:create {:submit {:success {:type :event
                                        ;; TODO implement
                                        :event :site-create}
                              :error {:type :event
                                      :event :site-error}}}}})

(defsmith survey smiths
  [state]
  {:resource {:type :survey
              :title ((:translate state) :survey/title)
              :endpoint "/survey"}
   :sidebar {:resource {:endpoint "/site"
                        :title ((:translate state) :survey/sidebar-title)
                        :type :site
                        :id :site_id
                        :label :site_name}}
   :layout [[:survey-name]
            [:survey-directory]]
   :schema {:survey-name {:type :text}
            :survey-directory {:type :text}}
   :states {:create {:submit {:success {:type :event
                                        ;; TODO implement
                                        :event :survey-create}
                              :error {:type :event
                                      :event :survey-error}}}}})

(defsmith settings smiths
  [state]
  {:resource {:type :settings
              :title ((:translate state) :settings/title)
              :endpoint "/settings"}
   :layout [[:label ((:translate state) :settings/preferences)]
            [:language]
            [:label ((:translate state) :settings/survey-settings)]
            [:root-path]
            [:project-start]
            [:project-end]
            [:required-fields]
            [:surveyed-species]
            [:night-start-hour]
            [:night-end-hour]
            [:sighting-independence-minutes-threshold]
            [:infrared-iso-value-threshold]
            [:erroneous-infrared-threshold]]
   :schema {:erroneous-infrared-threshold {:type :percentage}
            :infrared-iso-value-threshold {:type :number}
            :sighting-independence-minutes-threshold {:type :number}
            :language {:type :select
                       :options {:en ((:translate state) :language/en)
                                 :vn ((:translate state) :language/vn)}}
            :night-start-hour {:type :select
                               :options {17 "17:00"
                                         18 "18:00"
                                         19 "19:00"
                                         20 "20:00"
                                         21 "21:00"
                                         22 "22:00"
                                         23 "23:00"}}
            :night-end-hour {:type :select
                             :options {0 "0:00"
                                       1 "1:00"
                                       2 "2:00"
                                       3 "3:00"
                                       4 "4:00"
                                       5 "5:00"
                                       6 "6:00"
                                       7 "7:00"
                                       8 "8:00"}}
            :project-start {:type :datetime}
            :project-end {:type :datetime}
            :root-path {:type :string}
            :surveyed-species {:type :list
                               :list-of :string}
            :required-fields {:type :list
                              :list-of :paths
                              :complete-with :metadata}
            :rename {:type :setting-group
                     :group {:format {:type :string}
                             :fields {:type :list
                                      :list-of :paths
                                      :complete-with :metadata}
                             :date-format {:type :string}}}}
   :states {:update {:submit {:success {:type :event
                                        :event :settings-save}
                              :error {:type :event
                                      :event :settings-error}}
                     :cancel {:type :event
                              :event :settings-cancel}}}})
