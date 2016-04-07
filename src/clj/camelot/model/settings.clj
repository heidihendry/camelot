(ns camelot.model.settings)

(defn config-schema
  [state]
  {:erroneous-infrared-threshold {:type :percentage}
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
                    :date-format {:type :string}}}})

(def config-menu
  [[:label :settings/preferences]
   [:language]
   [:label :settings/survey-settings]
   [:root-path]
   [:project-start]
   [:project-end]
   [:required-fields]
   [:surveyed-species]
   [:night-start-hour]
   [:night-end-hour]
   [:sighting-independence-minutes-threshold]
   [:infrared-iso-value-threshold]
   [:erroneous-infrared-threshold]])

(def metadata-structure
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
