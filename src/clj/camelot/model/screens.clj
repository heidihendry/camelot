(ns camelot.model.screens
  (:require [camelot.smithy.core :refer [defsmith] :as smithy]
            [camelot.handler.camera-statuses :as camstat]
            [camelot.handler.survey-sites :as survey-sites]
            [camelot.handler.sites :as sites]))

(def smiths (atom {}))

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
              :endpoint "/sites"
              :id :site-id}
   :sidebar {:resource {:endpoint "/sites"
                        :title ((:translate state) :site/sidebar-title)
                        :type :site
                        :id :site-id
                        :label :site-name}}
   :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/delete)
                        :action :delete}
                       {:label ((:translate state) :action/edit)
                        :action :edit-mode}]}
   :layout [[:site-name]
            [:site-sublocation]
            [:site-city]
            [:site-state-province]
            [:site-country]
            [:site-notes]]
   :schema {:site-name {:type :text
                        :required true}
            :site-sublocation {:type :text}
            :site-city {:type :text}
            :site-state-province {:type :text}
            :site-country {:type :text}
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
                :endpoint "/cameras"
                :id :camera-id}
     :sidebar {:resource {:endpoint "/cameras"
                          :title ((:translate state) :camera/sidebar-title)
                          :type :camera
                          :id :camera-id
                          :label :camera-name}}
     :actionmenu {:title ((:translate state) :actionmenu/title)
                  :menu [{:label ((:translate state) :action/delete)
                          :action :delete}
                         {:label ((:translate state) :action/edit)
                          :action :edit-mode}]}
     :layout [[:camera-name]
              [:camera-status-id]
              [:camera-make]
              [:camera-model]
              [:camera-notes]]
     :schema {:camera-name {:type :text
                            :required true}
              :camera-status-id {:type :select
                                 :generator :camera-statuses
                                 :required true}
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
              :endpoint "/survey-sites"
              :parent-id-key :survey-id
              :id :survey-site-id}
   :sidebar {:resource {:endpoint "/survey-sites/survey"
                        :title ((:translate state) :survey-site/sidebar-title)
                        :type :survey-site
                        :id :survey-site-id
                        :label :site-name}}
   :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/delete)
                        :action :delete}
                       {:label ((:translate state) :action/trap-stations)
                        :action :trap-stations}]}
   :layout [[:site-id]]
   :schema {:site-id {:type :select
                      :required true
                      :generator :survey-sites-available}}
   :states {:create {:submit {:success {:type :event
                                        :event :survey-site-create}
                              :error {:type :event
                                      :event :survey-site-error}}}}})

(defsmith trap-station-session-camera smiths
  [state]
  {:resource {:type :trap-station-session-camera
              :title ((:translate state) :trap-station-session-camera/title)
              :endpoint "/trap-station-session-cameras"
              :parent-id-key :trap-station-session-id
              :id :trap-station-session-camera-id}
   :sidebar {:resource {:endpoint "/trap-station-session-cameras/trap-station-session"
                        :title ((:translate state) :trap-station-session-camera/sidebar-title)
                        :type :trap-station-session-camera
                        :id :trap-station-session-camera-id
                        :label :camera-name}}
   :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/delete)
                        :action :delete}
                       {:label ((:translate state) :action/import-media)
                        :action :import-media}]}
   :layout [[:camera-id]]
   :schema {:camera-id {:type :select
                        :required true
                        :generator :trap-station-session-cameras-available}}
   :states {:create {:submit {:success {:type :event
                                        :event :trap-station-session-camera-create}
                              :error {:type :event
                                      :event :trap-station-session-camera-error}}}}})

(defsmith trap-station smiths
  [state]
  {:resource {:type :trap-station
              :title ((:translate state) :trap-station/title)
              :endpoint "/trap-stations"
              :parent-id-key :survey-site-id
              :id :trap-station-id}
   :sidebar {:resource {:endpoint "/trap-stations/site"
                        :title ((:translate state) :trap-station/sidebar-title)
                        :type :trap-station
                        :id :trap-station-id
                        :label :trap-station-name}}
   :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/delete)
                        :action :delete}
                       {:label ((:translate state) :action/edit)
                        :action :edit-mode}
                       {:label ((:translate state) :action/sessions)
                        :action :trap-station-sessions}]}
   :layout [[:trap-station-name]
            [:trap-station-sublocation]
            [:trap-station-longitude]
            [:trap-station-latitude]
            [:trap-station-altitude]
            [:trap-station-notes]]
   :schema {:trap-station-name {:type :text
                                :required true}
            :trap-station-sublocation {:type :text}
            :trap-station-longitude {:type :number}
            :trap-station-latitude {:type :number}
            :trap-station-altitude {:type :number}
            :trap-station-notes {:type :textarea
                                 :cols 35
                                 :rows 4}}
   :states {:create {:submit {:success {:type :event
                                        :event :trap-station-create}
                              :error {:type :event
                                      :event :trap-station-error}}}}})

(defsmith trap-station-session smiths
  [state]
  {:resource {:type :trap-station-session
              :title ((:translate state) :trap-station-session/title)
              :endpoint "/trap-station-sessions"
              :parent-id-key :trap-station-id
              :id :trap-station-session-id}
   :sidebar {:resource {:endpoint "/trap-station-sessions/trap-station"
                        :title ((:translate state) :trap-station-session/sidebar-title)
                        :type :trap-station-session
                        :id :trap-station-session-id
                        :label :trap-station-session-label}}
   :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/delete)
                        :action :delete}
                       {:label ((:translate state) :action/edit)
                        :action :edit-mode}
                       {:label ((:translate state) :action/trap-station-session-cameras)
                        :action :trap-station-session-cameras}]}
   :layout [[:trap-station-session-start-date]
            [:trap-station-session-end-date]
            [:trap-station-session-notes]]
   :schema {:trap-station-session-start-date {:type :datetime
                                              :required true}
            :trap-station-session-end-date {:type :datetime
                                            :required true}
            :trap-station-session-notes {:type :textarea
                                 :cols 35
                                 :rows 4}}
   :states {:create {:submit {:success {:type :event
                                        :event :trap-station-session-create}
                              :error {:type :event
                                      :event :trap-station-session-error}}}}})

(defsmith survey smiths
  [state]
  {:resource {:type :survey
              :title ((:translate state) :survey/title)
              :endpoint "/surveys"
              :id :survey-id}
   :sidebar {:resource {:endpoint "/surveys"
                        :title ((:translate state) :survey/sidebar-title)
                        :type :survey
                        :id :survey-id
                        :label :survey-name}}
   :actionmenu {:title ((:translate state) :actionmenu/title)
                :menu [{:label ((:translate state) :action/delete)
                        :action :delete}
                       {:label ((:translate state) :action/edit)
                        :action :edit-mode}
                       {:label ((:translate state) :action/survey-sites)
                        :action :survey-sites}
                       ]}
   :layout [[:survey-name]
            [:survey-directory]
            [:survey-notes]]
   :schema {:survey-name {:type :text
                          :required true}
            :survey-directory {:type :text
                               :required true}
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
   :schema {:erroneous-infrared-threshold {:type :percentage
                                           :required true}
            :infrared-iso-value-threshold {:type :number
                                           :required true}
            :sighting-independence-minutes-threshold {:type :number
                                                      :required true}
            :language {:type :select
                       :required true
                       :options {:en ((:translate state) :language/en)
                                 :vn ((:translate state) :language/vn)}}
            :night-start-hour {:type :select
                               :required true
                               :options {17 "17:00"
                                         18 "18:00"
                                         19 "19:00"
                                         20 "20:00"
                                         21 "21:00"
                                         22 "22:00"
                                         23 "23:00"}}
            :night-end-hour {:type :select
                             :required true
                             :options {0 "0:00"
                                       1 "1:00"
                                       2 "2:00"
                                       3 "3:00"
                                       4 "4:00"
                                       5 "5:00"
                                       6 "6:00"
                                       7 "7:00"
                                       8 "8:00"}}
            :project-start {:type :datetime
                            :required true}
            :project-end {:type :datetime
                          :required true}
            :root-path {:type :string
                        :required true}
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
