(ns camelot.model.screens
  (:require [camelot.smithy.core :refer [defsmith] :as smithy]
            [camelot.handler.camera-status :as camstat]
            [camelot.handler.survey-sites :as survey-sites]
            [camelot.handler.sites :as sites]))

(defn translate-fn
  [state]
  (fn [resource lookup]
    ((:translate state) (keyword (format "%s/%s" (name resource) (subs (str lookup) 1))))))

(def smiths (atom {}))

;;(settings-screen (settings/gen-state settings/default-config))

(defn build-options
  [state data idkey transkey]
  (merge {"" ""}
         (into {} (map #(hash-map (get % idkey)
                                  ((:translate state) (keyword (get % transkey))))
                       data))))

(defsmith site smiths
  [state]
  {:resource {:type :site
              :title ((:translate state) :site/title)
              :endpoint "/site"
              :id :site-id}
   :sidebar {:resource {:listing-endpoint "/sites"
                        :specific-endpoint "/site"
                        :title ((:translate state) :site/sidebar-title)
                        :type :site
                        :id :site-id
                        :label :site-name}}
   :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/edit)
                        :action :edit-mode}]}
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
            :site-notes {:type :textarea
                         :rows 4
                         :cols 35}}
   :states {:create {:submit {:success {:type :event
                                        ;; TODO implement
                                        :event :site-create-success}
                              :error {:type :event
                                      :event :site-create-error}}}
            :update {:submit {:success {:type :event
                                        :event :site-update-success}
                                :error {:type :event
                                        :event :site-update-error}}}}})

(defsmith camera smiths
  [state]
  (let [camstats (camstat/get-all state)
        opts (build-options state camstats :camera-status-id :camera-status-description)]
    {:resource {:type :camera
                :title ((:translate state) :camera/title)
                :endpoint "/camera"
                :id :camera-id}
     :sidebar {:resource {:listing-endpoint "/cameras"
                          :specific-endpoint "/camera"
                          :title ((:translate state) :camera/sidebar-title)
                          :type :camera
                          :id :camera-id
                          :label :camera-name}}
     :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/edit)
                        :action :edit-mode}]}
     :layout [[:camera-name]
              [:camera-status]
              [:camera-make]
              [:camera-model]
              [:camera-notes]]
     :schema {:camera-name {:type :text}
              :camera-status {:type :select
                              ;; TODO pull values from DB
                              :options opts}
              :camera-make {:type :text}
              :camera-model {:type :text}
              :camera-notes {:type :textarea
                             :rows 4
                             :cols 35}}
     :states {:create {:submit {:success {:type :event
                                          ;; TODO implement
                                          :event :camera-create-success}
                                :error {:type :event
                                        :event :camera-create-error}}}
              :update {:submit {:success {:type :event
                                          :event :camera-update-success}
                                :error {:type :event
                                        :event :camera-update-error}}}}}))

(defsmith survey-site smiths
  [state]
  {:resource {:type :survey-site
              :title ((:translate state) :survey-site/title)
              :endpoint "/survey-site"
              :parent-id-key :survey-id
              :id :survey-site-id}
   :sidebar {:resource {:listing-endpoint "/survey-sites"
                        :specific-endpoint "/survey-site"
                        :title ((:translate state) :survey-site/sidebar-title)
                        :type :survey-site
                        :id :survey-site-id
                        :label :site-name}}
   :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/edit)
                        :action :edit-mode}]}
   :layout [[:site-id]]
   :schema {:site-id {:type :select
                      :options (conj {"" ""}
                                     (into {} (map #(hash-map (str (:site-id %)) (:site-name %))
                                                   (sites/get-all state))))}}
   :states {:create {:submit {:success {:type :event
                                        :event :survey-site-create}
                              :error {:type :event
                                      :event :survey-site-error}}}}})

(defsmith survey smiths
  [state]
  {:resource {:type :survey
              :title ((:translate state) :survey/title)
              :endpoint "/survey"
              :id :survey-id}
   :sidebar {:resource {:listing-endpoint "/surveys"
                        :specific-endpoint "/survey"
                        :title ((:translate state) :survey/sidebar-title)
                        :type :survey
                        :id :survey-id
                        :label :survey-name}}
   :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/edit)
                        :action :edit-mode}
                       {:label ((:translate state) :action/survey-sites)
                        :action :survey-sites}
                       ]}
   :layout [[:survey-name]
            [:survey-directory]
            [:survey-notes]]
   :schema {:survey-name {:type :text}
            :survey-directory {:type :text}
            :survey-notes {:type :textarea
                           :rows 4
                           :cols 35}}
   :states {:create {:submit {:success {:type :event
                                        ;; TODO implement
                                        :event :survey-create}
                              :error {:type :event
                                      :event :survey-error}}}
            :update {:submit {:success {:type :event
                                        ;; TODO implement
                                        :event :survey-update-success}
                              :error {:type :event
                                      :event :survey-update-error}}}}})

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

(defn smith
  [state]
  (smithy/build-smiths smiths (translate-fn state) state))
