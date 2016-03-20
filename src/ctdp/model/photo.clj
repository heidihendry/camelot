(ns ctdp.model.photo
  (:require [schema.core :as s]))

(s/defrecord CameraSettings
    [aperture :- s/Str
     exposure :- s/Str
     flash :- s/Str
     focal-length :- s/Str
     fstop :- s/Str
     iso :- s/Num
     resolution-x :- s/Num
     resolution-y :- s/Num])

(s/defrecord Camera
    [make :- s/Str
     model :- s/Str
     software :- s/Str])

(s/defrecord PhotoMetadata
    [datetime :- org.joda.time.DateTime
     description :- s/Str
     filesize :- s/Num
     camera :- Camera
     settings :- CameraSettings])

(s/defn camera :- Camera
  "Camera constructor"
  [{:keys [make model sw]}]
  {:pre [(string? make)
         (string? model)
         (string? sw)]}
  (->Camera make model sw))

(s/defn camera-settings :- CameraSettings
  "CameraSettings constructor"
  [{:keys [aperture exposure flash focal-length fstop iso width height]}]
  {:pre [(string? aperture)
         (string? exposure)
         (string? flash)
         (string? focal-length)
         (string? fstop)
         (number? iso)
         (number? width)
         (number? height)]}
  (->CameraSettings aperture exposure flash focal-length
                    fstop iso width height))

(s/defn photo :- PhotoMetadata
  "Photo constructor"
  [{:keys [datetime description filesize camera camera-settings]}]
  {:pre [(instance? org.joda.time.DateTime datetime)
         (string? description)
         (number? filesize)
         (instance? Camera camera)
         (instance? CameraSettings camera-settings)]}
  (->PhotoMetadata datetime description filesize camera camera-settings))
