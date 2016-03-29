(ns camelot.model.photo
  (:require [schema.core :as s]))

(s/defrecord Location
    [gps-longitude :- s/Str
     gps-longitude-ref :- s/Str
     gps-latitude :- s/Str
     gps-latitude-ref :- s/Str
     gps-altitude :- s/Str
     gps-altitude-ref :- s/Str
     sublocation :- s/Str
     city :- s/Str
     state-province :- s/Str
     country :- s/Str
     country-code :- s/Str
     map-datum :- s/Num])

(s/defrecord CameraSettings
    [aperture :- s/Str
     exposure :- s/Str
     flash :- s/Str
     focal-length :- s/Str
     fstop :- s/Str
     iso :- s/Num
     orientation :- s/Str
     resolution-x :- s/Num
     resolution-y :- s/Num])

(s/defrecord Camera
    [make :- s/Str
     model :- s/Str
     software :- s/Str])

(s/defrecord Sighting
    [species :- s/Str
     quantity :- s/Num])

(s/defrecord PhotoMetadata
    [datetime :- org.joda.time.DateTime
     headline :- s/Str
     artist :- s/Str
     phase :- s/Str
     copyright :- s/Str
     description :- s/Str
     filename :- s/Str
     filesize :- s/Num
     sightings :- [Sighting]
     camera :- Camera
     settings :- CameraSettings
     location :- Location])

(s/defn location :- Location
  [{:keys [gps-lon gps-lon-ref gps-lat gps-lat-ref gps-alt gps-alt-ref subloc city state country country-code map-datum]}]
  {:pre [(string? gps-lon)
         (string? gps-lon-ref)
         (string? gps-lat)
         (string? gps-lat-ref)
         (string? gps-alt)
         (string? gps-alt-ref)
         (string? subloc)
         (string? city)
         (string? state)
         (string? country)
         (string? country-code)
         (string? map-datum)]}
  (->Location gps-lon gps-lon-ref gps-lat gps-lat-ref gps-alt gps-alt-ref
              subloc city state country country-code map-datum))\

(s/defn sighting :- Sighting
  [{:keys [species quantity]}]
  {:pre [(string? species)
         (number? quantity)]}
  (->Sighting species quantity))

(s/defn camera :- Camera
  "Camera constructor"
  [{:keys [make model sw]}]
  {:pre [(string? make)
         (string? model)
         (string? sw)]}
  (->Camera make model sw))

(s/defn camera-settings :- CameraSettings
  "CameraSettings constructor"
  [{:keys [aperture exposure flash focal-length fstop iso orientation width height]}]
  {:pre [(string? aperture)
         (string? exposure)
         (string? flash)
         (string? focal-length)
         (string? fstop)
         (number? iso)
         (string? orientation)
         (number? width)
         (number? height)]}
  (->CameraSettings aperture exposure flash focal-length
                    fstop iso orientation width height))

(s/defn photo :- PhotoMetadata
  "Photo constructor"
  [{:keys [datetime headline artist phase copyright description filename filesize sightings camera camera-settings location]}]
  {:pre [(instance? org.joda.time.DateTime datetime)
         (string? description)
         (number? filesize)
         (s/validate [Sighting] sightings)
         (instance? Camera camera)
         (instance? CameraSettings camera-settings)
         (instance? Location location)]}
  (->PhotoMetadata datetime headline artist phase copyright description filename filesize sightings camera camera-settings location))
