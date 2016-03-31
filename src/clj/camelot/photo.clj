(ns camelot.photo
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [schema.core :as s]
            [camelot.model.photo :as mp])
  (:import [camelot.model.photo Camera CameraSettings PhotoMetadata]))

(s/defn night? :- s/Bool
  "Check whether the given time is 'night'."
  [night-start night-end hour]
  (or (> hour night-start) (< hour night-end)))

(s/defn infrared-sane? :- s/Bool
  "Check whether the infraresh thresholds for a photo seem valid."
  [nightfn isothresh photo]
  (let [hour (t/hour (:datetime photo))
        iso (:iso (:settings photo))]
    (or (> iso isothresh) (not (nightfn hour)))))

(s/defn exif-date-to-datetime :- org.joda.time.DateTime
  "Exif metadata dates are strings like 2014:04:11 16:37:00.  This makes them real dates."
  [ed]
  (let [parts (str/split ed #"[ :]")]
    (assert (= (count parts) 6))
    (apply t/date-time (map #(Integer/parseInt %) parts))))

(s/defn normalise :- PhotoMetadata
  "Return a normalised data structure for the given vendor- and photo-specific metadata"
  [state metadata]
  (let [md #(get metadata %)
        cam (mp/camera
             {:make (md "Make")
              :model (md "Model")
              :sw (md "Software")})
        camset (mp/camera-settings
                {:aperture (md "Aperture Value")
                 :exposure (md "Exposure Time")
                 :flash (md "Flash")
                 :focal-length (md "Focal Length")
                 :fstop (md "F-Number")
                 :iso (read-string (md "ISO Speed Ratings"))
                 :orientation (md "Orientation")
                 :width (read-string (md "Image Width"))
                 :height (read-string (md "Image Height"))})
        location (mp/location
                  {:gps-lon (md "GPS Longitude")
                   :gps-lon-ref (md "GPS Longitude Ref")
                   :gps-lat (md "GPS Latitude")
                   :gps-lat-ref (md "GPS Latitude Ref")
                   :gps-alt (md "GPS Altitude")
                   :gps-alt-ref (md "GPS Altitude Ref")
                   :subloc (md "Sub-location")
                   :city (md "City")
                   :state (md "Province/State")
                   :country (md "Country/Primary Location Name")
                   :country-code (md "Country/Primary Location Code")
                   :map-datum (md "GPS Map Datum")})]
    (mp/photo
     {:camera-settings camset
      :camera cam
      :sightings (if (or (md "Caption/Abstract") (md "Object Name"))
                   [(mp/sighting {:species (md "Caption/Abstract")
                                  :quantity (md "Object Name")})]
                   [])
      :datetime (exif-date-to-datetime (md "Date/Time"))
      :headline (md "Headline")
      :artist (md "Artist")
      :phase (md "Source")
      :copyright (md "Copyright Notice")
      :description (md "Description")
      :filename (md "File Name")
      :filesize (read-string (md "File Size"))
      :location location})))

(defn extract-path-value
  [metadata path]
  (reduce (fn [acc n] (get acc n)) metadata path))
