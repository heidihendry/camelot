(ns camelot.model.photo
  (:require [schema.core :as s]))

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

(s/defrecord Location
    [gps-longitude :- s/Str
     gps-longitude-ref :- s/Str
     gps-latitude :- s/Str
     gps-latitude-ref :- s/Str
     gps-altitude :- (s/maybe s/Str)
     gps-altitude-ref :- (s/maybe s/Str)
     sublocation :- (s/maybe s/Str)
     city :- (s/maybe s/Str)
     state-province :- (s/maybe s/Str)
     country :- (s/maybe s/Str)
     country-code :- (s/maybe s/Str)
     map-datum :- (s/maybe s/Num)])

(s/defrecord CameraSettings
    [aperture :- (s/maybe s/Str)
     exposure :- s/Str
     flash :- s/Str
     focal-length :- (s/maybe s/Str)
     fstop :- s/Str
     iso :- s/Num
     orientation :- (s/maybe s/Str)
     resolution-x :- s/Num
     resolution-y :- s/Num])

(s/defrecord Camera
    [make :- s/Str
     model :- s/Str
     software :- (s/maybe s/Str)])

(s/defrecord Sighting
    [species :- s/Str
     quantity :- s/Num])

(s/defrecord PhotoMetadata
    [datetime :- org.joda.time.DateTime
     headline :- (s/maybe s/Str)
     artist :- (s/maybe s/Str)
     phase :- (s/maybe s/Str)
     copyright :- (s/maybe s/Str)
     description :- (s/maybe s/Str)
     filename :- s/Str
     filesize :- s/Num
     sightings :- [Sighting]
     camera :- (s/maybe Camera)
     settings :- (s/maybe CameraSettings)
     location :- Location])

(s/defn location :- Location
  [{:keys [gps-lon gps-lon-ref gps-lat gps-lat-ref gps-alt gps-alt-ref subloc city state country country-code map-datum]}]
  (->Location gps-lon gps-lon-ref gps-lat gps-lat-ref gps-alt gps-alt-ref
              subloc city state country country-code map-datum))

(s/defn sighting :- Sighting
  [{:keys [species quantity]}]
  (->Sighting species quantity))

(s/defn camera :- Camera
  "Camera constructor"
  [{:keys [make model sw]}]
  (->Camera make model sw))

(s/defn camera-settings :- CameraSettings
  "CameraSettings constructor"
  [{:keys [aperture exposure flash focal-length fstop iso orientation width height]}]
  (->CameraSettings aperture exposure flash focal-length
                    fstop iso orientation width height))

(s/defn photo :- PhotoMetadata
  "Photo constructor"
  [{:keys [datetime headline artist phase copyright description filename filesize sightings camera camera-settings location]}]
  (->PhotoMetadata datetime headline artist phase copyright description filename filesize sightings camera camera-settings location))
