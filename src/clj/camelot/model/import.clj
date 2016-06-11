(ns camelot.model.import
  (:require [schema.core :as s]))

(defn valid-photo?
  [photo]
  (not (:invalid photo)))

(def ImportInvalidPhoto
  {(s/required-key :invalid) s/Str})

(s/defrecord ImportLocation
    [gps-longitude :- (s/maybe s/Num)
     gps-latitude :- (s/maybe s/Num)
     gps-altitude :- (s/maybe s/Str)
     sublocation :- (s/maybe s/Str)
     city :- (s/maybe s/Str)
     state-province :- (s/maybe s/Str)
     country :- (s/maybe s/Str)
     country-code :- (s/maybe s/Str)
     map-datum :- (s/maybe s/Str)])

(s/defrecord ImportCameraSettings
    [aperture :- (s/maybe s/Str)
     exposure :- (s/maybe s/Str)
     flash :- (s/maybe s/Str)
     focal-length :- (s/maybe s/Str)
     fstop :- (s/maybe s/Str)
     iso :- (s/maybe s/Num)
     orientation :- (s/maybe s/Str)
     resolution-x :- s/Num
     resolution-y :- s/Num])

(s/defrecord ImportCamera
    [make :- (s/maybe s/Str)
     model :- (s/maybe s/Str)
     software :- (s/maybe s/Str)])

(s/defrecord ImportSighting
    [species :- (s/maybe s/Str)
     quantity :- (s/maybe s/Num)])

(s/defrecord ImportPhotoMetadata
    [datetime :- org.joda.time.DateTime
     datetime-original :- (s/maybe org.joda.time.DateTime)
     headline :- (s/maybe s/Str)
     artist :- (s/maybe s/Str)
     phase :- (s/maybe s/Str)
     copyright :- (s/maybe s/Str)
     description :- (s/maybe s/Str)
     filename :- s/Str
     filesize :- s/Num
     sightings :- [ImportSighting]
     camera :- (s/maybe ImportCamera)
     settings :- (s/maybe ImportCameraSettings)
     location :- ImportLocation])

(s/defn location :- ImportLocation
  [{:keys [gps-longitude
           gps-latitude
           gps-altitude
           sublocation
           city
           state
           country
           country-code
           map-datum]}]
  (->ImportLocation gps-longitude
                    gps-latitude
                    gps-altitude
                    sublocation
                    city
                    state
                    country
                    country-code
                    map-datum))

(s/defn sighting :- ImportSighting
  [{:keys [species quantity]}]
  (->ImportSighting species quantity))

(s/defn camera :- ImportCamera
  "Camera constructor"
  [{:keys [make model sw]}]
  (->ImportCamera make model sw))

(s/defn camera-settings :- ImportCameraSettings
  "CameraSettings constructor"
  [{:keys [aperture exposure flash focal-length fstop iso orientation width
           height]}]
  (->ImportCameraSettings aperture exposure flash focal-length fstop iso
                          orientation width height))

(s/defn photo :- ImportPhotoMetadata
  "Photo constructor"
  [{:keys [datetime datetime-original headline artist phase copyright
  description filename filesize sightings camera camera-settings location]}]
  (->ImportPhotoMetadata datetime datetime-original headline artist phase
                         copyright description filename filesize sightings
                         camera camera-settings location))

(def ImportRawMetadata {s/Str s/Str})

(def ImportIndependentSightings
  "Sighting counts keyed by species."
  {(s/required-key :species) s/Str
   (s/required-key :count) s/Num})

(def ImportExtractedMetadata
  "Summary of the Album's contents."
  {(s/required-key :datetime-start) org.joda.time.DateTime
   (s/required-key :datetime-end) org.joda.time.DateTime
   (s/required-key :make) (s/maybe s/Str)
   (s/required-key :model) (s/maybe s/Str)
   (s/optional-key :sightings) ImportIndependentSightings})

(def ImportProblem
  "A single problem with the album."
  {(s/required-key :result) s/Keyword
   (s/required-key :reason) s/Str})

(def ImportAlbum
  "Album.  If not invalid (:invalid true), it will contain a metadata summary.) "
  {(s/required-key :photos) {java.io.File (s/if valid-photo?
                                            ImportPhotoMetadata
                                            ImportInvalidPhoto)}
   (s/optional-key :metadata) ImportExtractedMetadata
   (s/optional-key :invalid) s/Bool
   (s/required-key :problems) [ImportProblem]})
