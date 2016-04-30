(ns camelot.model.photo
  (:require [schema.core :as s]))

(s/defrecord Location
    [gps-longitude :- (s/maybe s/Num)
     gps-latitude :- (s/maybe s/Num)
     gps-altitude :- (s/maybe s/Str)
     sublocation :- (s/maybe s/Str)
     city :- (s/maybe s/Str)
     state-province :- (s/maybe s/Str)
     country :- (s/maybe s/Str)
     country-code :- (s/maybe s/Str)
     map-datum :- (s/maybe s/Str)])

(s/defrecord CameraSettings
    [aperture :- (s/maybe s/Str)
     exposure :- (s/maybe s/Str)
     flash :- (s/maybe s/Str)
     focal-length :- (s/maybe s/Str)
     fstop :- (s/maybe s/Str)
     iso :- (s/maybe s/Num)
     orientation :- (s/maybe s/Str)
     resolution-x :- s/Num
     resolution-y :- s/Num])

(s/defrecord Camera
    [make :- (s/maybe s/Str)
     model :- (s/maybe s/Str)
     software :- (s/maybe s/Str)])

(s/defrecord Sighting
    [species :- (s/maybe s/Str)
     quantity :- (s/maybe s/Num)])

(s/defrecord PhotoMetadata
    [datetime :- org.joda.time.DateTime
     datetime-original :- (s/maybe org.joda.time.DateTime)
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
  [{:keys [gps-lon gps-lat gps-alt subloc city state country country-code map-datum]}]
  (->Location gps-lon gps-lat gps-alt subloc city state country country-code map-datum))

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
  [{:keys [datetime datetime-original headline artist phase copyright description filename filesize sightings camera camera-settings location]}]
  (->PhotoMetadata datetime datetime-original headline artist phase copyright description filename filesize sightings camera camera-settings location))
