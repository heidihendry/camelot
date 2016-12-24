(ns camelot.system.screens
  (:require
   [smithy.core :refer [defsmith] :as smithy]
   [camelot.import.util :as putil]
   [environ.core :refer [env]]
   [camelot.translation.core :as tr]))

(def smiths (atom {}))

(def metadata-structure
  "Layout of all photo metadata fields."
  [[:location [:gps-longitude
               :gps-longitude-ref
               :gps-latitude
               :gps-latitude-ref
               :gps-altitude
               :gps-altitude-ref
               :sublocation
               :city
               :state-province
               :country
               :country-code
               :map-datum]]
   [:camera-settings [:aperture
                      :exposure
                      :flash
                      :focal-length
                      :fstop
                      :iso
                      :orientation
                      :resolution-x
                      :resolution-y]]
   [:camera [:make
             :model
             :software]]
   :datetime
   :headline
   :artist
   :phase
   :copyright
   :description
   :filename
   :filesize])

(defn nav-menu
  "Main navigation menu structure."
  [state]
  (if (env :camelot-legacy-features)
    {:menu-items [{:url "/organisation"
                   :label (tr/translate state :application/organisation)}
                  {:url "/library"
                   :label (tr/translate state :application/library)}
                  {:url "/dashboard"
                   :label (tr/translate state :application/import)}
                  {:function "settings"}
                  {:function "bulk-import-progress"}]}
    {:menu-items [{:url "/organisation"
                   :label (tr/translate state :application/organisation)}
                  {:url "/library"
                   :label (tr/translate state :application/library)}
                  {:function "settings"}
                  {:function "bulk-import-progress"}]}))

(defn- translate-fn
  "Return a key translation function for the smithy build process."
  [state]
  (fn [resource lookup]
    (tr/translate state (keyword (format "%s/%s"
                                                   (name resource)
                                                   (subs (str lookup) 1))))))

(defn all-screens
  "Build the available screen smiths."
  [state]
  (smithy/build-smiths smiths (translate-fn state) state))

(defsmith site smiths
  [state]
  {:resource {:type :site
              :title (tr/translate state :site/title)
              :endpoint "/sites"
              :id :site-id}
   :sidebar {:resource {:endpoint "/sites"
                        :title (tr/translate state :site/sidebar-title)
                        :type :site
                        :id :site-id
                        :label :site-name}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :words/edit)
                        :action :edit-mode}]}
   :layout [[:site-name]
            [:site-sublocation]
            [:site-city]
            [:site-state-province]
            [:site-country]
            [:site-area]
            [:site-notes]]
   :schema {:site-name {:type :text
                        :required true}
            :site-sublocation {:type :text}
            :site-city {:type :text}
            :site-state-province {:type :text}
            :site-country {:type :text}
            :site-area {:type :number}
            :site-notes {:type :textarea
                         :rows 4
                         :cols 35}}
   :states {:create {:submit {:success {:type :event
                                        :event :site-create-success}
                              :error {:type :event
                                      :event :site-create-error}}}
            :update {:submit {:success {:type :event
                                        :event :site-update-success}
                                :error {:type :event
                                        :event :site-update-error}}}}})

(defsmith camera smiths
  [state]
  {:resource {:type :camera
              :title (tr/translate state :camera/title)
              :endpoint "/cameras"
              :id :camera-id}
   :sidebar {:resource {:endpoint "/cameras"
                        :title (tr/translate state :camera/sidebar-title)
                        :type :camera
                        :id :camera-id
                        :label :camera-name}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :words/edit)
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
                                        :event :camera-create-success}
                              :error {:type :event
                                      :event :camera-create-error}}}
            :update {:submit {:success {:type :event
                                        :event :camera-update-success}
                              :error {:type :event
                                      :event :camera-update-error}}}}})

(defsmith survey-site smiths
  [state]
  {:resource {:type :survey-site
              :title (tr/translate state :survey-site/title)
              :endpoint "/survey-sites"
              :parent-id-key :survey-id
              :id :survey-site-id}
   :sidebar {:resource {:endpoint "/survey-sites/survey"
                        :title (tr/translate state :survey-site/sidebar-title)
                        :type :survey-site
                        :id :survey-site-id
                        :label :site-name}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :action/trap-stations)
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
              :title (tr/translate state :trap-station-session-camera/title)
              :endpoint "/trap-station-session-cameras"
              :parent-id-key :trap-station-session-id
              :id :trap-station-session-camera-id}
   :sidebar {:resource {:endpoint "/trap-station-session-cameras/trap-station-session"
                        :title (tr/translate state :trap-station-session-camera/sidebar-title)
                        :type :trap-station-session-camera
                        :id :trap-station-session-camera-id
                        :label :camera-name}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state
                                             :trap-station-session-camera/delete-media)
                        :action :delete-media}
                       {:label (tr/translate state :words/edit)
                        :action :edit-mode}
                       {:label (tr/translate state :action/media)
                        :action :media}]}
   :layout [[:camera-id]
            [:trap-station-session-camera-media-unrecoverable]]
   :schema {:camera-id {:type :select
                        :required true
                        :generator :trap-station-session-cameras-available}
            :trap-station-session-camera-media-unrecoverable {:type :boolean
                                                              :required true}}
   :states {:create {:submit {:success {:type :event
                                        :event :trap-station-session-camera-create}
                              :error {:type :event
                                      :event :trap-station-session-camera-error}}}}})

(defsmith trap-station smiths
  [state]
  {:resource {:type :trap-station
              :title (tr/translate state :trap-station/title)
              :endpoint "/trap-stations"
              :parent-id-key :survey-site-id
              :id :trap-station-id}
   :sidebar {:resource {:endpoint "/trap-stations/site"
                        :title (tr/translate state :trap-station/sidebar-title)
                        :type :trap-station
                        :id :trap-station-id
                        :label :trap-station-name}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :words/edit)
                        :action :edit-mode}
                       {:label (tr/translate state :action/sessions)
                        :action :trap-station-sessions}]}
   :layout [[:trap-station-name]
            [:trap-station-longitude]
            [:trap-station-latitude]
            [:trap-station-altitude]
            [:trap-station-distance-above-ground]
            [:trap-station-distance-to-road]
            [:trap-station-distance-to-river]
            [:trap-station-distance-to-settlement]
            [:trap-station-notes]]
   :schema {:trap-station-name {:type :text
                                :required true}
            :trap-station-longitude {:type :number}
            :trap-station-latitude {:type :number}
            :trap-station-altitude {:type :number}
            :trap-station-distance-above-ground {:type :number}
            :trap-station-distance-to-road {:type :number}
            :trap-station-distance-to-river {:type :number}
            :trap-station-distance-to-settlement {:type :number}
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
              :title (tr/translate state :trap-station-session/title)
              :endpoint "/trap-station-sessions"
              :parent-id-key :trap-station-id
              :id :trap-station-session-id}
   :sidebar {:resource {:endpoint "/trap-station-sessions/trap-station"
                        :title (tr/translate state :trap-station-session/sidebar-title)
                        :type :trap-station-session
                        :id :trap-station-session-id
                        :label :trap-station-session-label}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :words/edit)
                        :action :edit-mode}
                       {:label (tr/translate state :action/trap-station-session-cameras)
                        :action :trap-station-session-cameras}]}
   :layout [[:trap-station-session-start-date]
            [:trap-station-session-end-date]
            [:trap-station-session-notes]]
   :schema {:trap-station-session-start-date {:type :datetime
                                              :required true}
            :trap-station-session-end-date {:type :datetime}
            :trap-station-session-notes {:type :textarea
                                 :cols 35
                                 :rows 4}}
   :states {:create {:submit {:success {:type :event
                                        :event :trap-station-session-create}
                              :error {:type :event
                                      :event :trap-station-session-error}}}}})

(defsmith media smiths
  [state]
  {:resource {:type :media
              :title (tr/translate state :media/title)
              :endpoint "/media"
              :non-creatable true
              :parent-id-key :camera-trap-session-camera-id
              :id :media-id}
   :sidebar {:resource {:endpoint "/media/trap-station-session-camera"
                        :title (tr/translate state :media/sidebar-title)
                        :type :media
                        :id :media-id
                        :label :media-capture-timestamp-label}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :words/edit)
                        :action :edit-mode}
                       {:label (tr/translate state :action/sightings)
                        :action :sightings}
                       {:label (tr/translate state :action/photo)
                        :action :photos}]}
   :layout [[:media-filename]
            [:media-capture-timestamp]
            [:media-notes]]
   :schema {:media-filename {:type :image}
            :media-capture-timestamp {:type :datetime
                                      :required true
                                      :detailed true}
            :media-notes {:type :textarea}}
   :states {:create {:submit {:success {:type :event
                                        :event :media-create}
                              :error {:type :event
                                      :event :media-error}}}}})

(defsmith photo smiths
  [state]
  {:resource {:type :photo
              :title (tr/translate state :photo/title)
              :endpoint "/photos"
              :non-creatable true
              :parent-id-key :media-id
              :id :photo-id}
   :sidebar {:resource {:endpoint "/photos/media"
                        :title (tr/translate state :photo/sidebar-title)
                        :type :photo
                        :id :photo-id
                        :label :photo-created}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :words/edit)
                        :action :edit-mode}]}
   :layout [[:photo-iso-setting]
            [:photo-exposure-value]
            [:photo-flash-setting]
            [:photo-focal-length]
            [:photo-fnumber-setting]
            [:photo-orientation]
            [:photo-resolution-x]
            [:photo-resolution-y]]
   :schema {:photo-iso-setting {:type :number}
            :photo-exposure-value {:type :text}
            :photo-focal-length {:type :text}
            :photo-flash-setting {:type :text}
            :photo-fnumber-setting {:type :text}
            :photo-orientation {:type :text}
            :photo-resolution-x {:type :number}
            :photo-resolution-y {:type :number}}
   :states {:create {:submit {:success {:type :event
                                        :event :photo-create}
                              :error {:type :event
                                      :event :photo-error}}}}})

(defsmith sighting smiths
  [state]
  {:resource {:type :sighting
              :title (tr/translate state :sighting/title)
              :endpoint "/sightings"
              :parent-id-key :media-id
              :id :sighting-id}
   :sidebar {:resource {:endpoint "/sightings/media"
                        :title (tr/translate state :sighting/sidebar-title)
                        :type :sighting
                        :id :sighting-id
                        :label :sighting-label}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :words/edit)
                        :action :edit-mode}]}
   :layout [[:taxonomy-id]
            [:sighting-quantity]
            [:sighting-sex]
            [:sighting-lifestage]]
   :schema {:taxonomy-id {:type :select
                         :required true
                         :generator :taxonomy-available}
            :sighting-quantity {:type :number
                                :required true}
            :sighting-sex {:type :select
                           :required false
                           :options {"unidentified" (tr/translate state
                                                                        :sighting/sighting-property.unidentified)
                                     "M" (tr/translate state
                                                       :sighting/sighting-sex.male)
                                     "F" (tr/translate state
                                                       :sighting/sighting-sex.female)}}
            :sighting-lifestage {:type :select
                                 :required false
                                 :options {"unidentified" (tr/translate state
                                                                        :sighting/sighting-property.unidentified)
                                           "adult" (tr/translate state
                                                                 :sighting/sighting-lifestage.adult)
                                           "juvenile" (tr/translate state
                                                                    :sighting/sighting-lifestage.juvenile)}}}
   :states {:create {:submit {:success {:type :event
                                        :event :sighting-create}
                              :error {:type :event
                                      :event :sighting-error}}}}})

(defsmith taxonomy smiths
  [state]
  {:resource {:type :taxonomy
              :title (tr/translate state :taxonomy/title)
              :endpoint "/taxonomy"
              :id :taxonomy-id}
   :sidebar {:resource {:endpoint "/taxonomy"
                        :title (tr/translate state :taxonomy/sidebar-title)
                        :type :taxonomy
                        :id :taxonomy-id
                        :label :taxonomy-label}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :words/edit)
                        :action :edit-mode}]}
   :layout [[:taxonomy-class]
            [:taxonomy-order]
            [:taxonomy-family]
            [:taxonomy-genus]
            [:taxonomy-species]
            [:taxonomy-common-name]
            [:species-mass-id]
            [:taxonomy-notes]]
   :schema {:taxonomy-class {:type :text}
            :taxonomy-order {:type :text}
            :taxonomy-family {:type :text}
            :taxonomy-genus {:type :text
                             :required true}
            :taxonomy-species {:type :text
                               :required true}
            :taxonomy-common-name {:type :text
                                   :required true}
            :species-mass-id {:type :select
                              :generator :species-mass}
            :taxonomy-notes {:type :textarea
                            :cols 35
                            :rows 4}}
   :states {:create {:submit {:success {:type :event
                                        :event :taxonomy-create}
                              :error {:type :event
                                      :event :taxonomy-error}}}}})

(defsmith survey smiths
  [state]
  {:resource {:type :survey
              :title (tr/translate state :survey/title)
              :endpoint "/surveys"
              :id :survey-id}
   :sidebar {:resource {:endpoint "/surveys"
                        :title (tr/translate state :survey/sidebar-title)
                        :type :survey
                        :id :survey-id
                        :label :survey-name}}
   :actionmenu {:menu [{:label (tr/translate state :words/delete)
                        :action :delete}
                       {:label (tr/translate state :words/edit)
                        :action :edit-mode}
                       {:label (tr/translate state :action/survey-sites)
                        :action :survey-sites}]}
   :layout [[:survey-name]
            [:survey-notes]]
   :schema {:survey-name {:type :text
                          :required true}
            :survey-notes {:type :textarea
                           :rows 4
                           :cols 35}}
   :states {:create {:submit {:success {:type :event
                                        :event :survey-create}
                              :error {:type :event
                                      :event :survey-error}}}
            :update {:submit {:success {:type :event
                                        :event :survey-update-success}
                              :error {:type :event
                                      :event :survey-update-error}}}}})

(if (env :camelot-legacy-features)
  (defsmith settings smiths
    [state]
    {:resource {:type :settings
                :title (tr/translate state :settings/title)
                :endpoint "/settings"}
     :layout [[:label (tr/translate state :settings/preferences)]
              [:language]
              [:label (tr/translate state :settings/survey-settings)]
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
                         :options {:en (tr/translate state :language/en)
                                   :vn (tr/translate state :language/vn)}}
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
              :surveyed-species {:type :list
                                 :list-of :string}
              :required-fields {:type :list
                                :list-of :paths
                                :complete-with :metadata}}
     :states {:update {:submit {:success {:type :event
                                          :event :settings-legacy-save}
                                :error {:type :event
                                        :event :settings-error}}
                       :cancel {:type :event
                                :event :settings-cancel}}}})
  (defsmith settings smiths
    [state]
    {:resource {:type :settings
                :title (tr/translate state :settings/title)
                :endpoint "/settings"}
     :layout [[:label (tr/translate state :settings/survey-settings)]
              [:sighting-independence-minutes-threshold]
              [:send-usage-data]]
     :schema {:language {:type :select
                         :required true
                         :options {:en (tr/translate state :language/en)
                                   :vn (tr/translate state :language/vn)}}
              :send-usage-data {:type :boolean}
              :sighting-independence-minutes-threshold {:type :number
                                                        :required true}}
     :states {:update {:submit {:success {:type :event
                                          :event :settings-save}
                                :error {:type :event
                                        :event :settings-error}}
                       :cancel {:type :event
                                :event :settings-cancel}}}}))

(defn- flatten-metadata-structure
  "Transfor metadata structure in to a vector of paths"
  [md]
  (vec (reduce #(into %1 (if (= (type %2) clojure.lang.Keyword)
                              [[%2]]
                              (mapv (fn [v] [(first %2) v]) (second %2))))
                   []
                   md)))

(def metadata-paths (flatten-metadata-structure metadata-structure))

(defn get-metadata
  "Return paths alongside a (translated) description of the metadata represented
  by that path."
  [state]
  (into {} (map #(hash-map % (putil/path-description state %)) metadata-paths)))
