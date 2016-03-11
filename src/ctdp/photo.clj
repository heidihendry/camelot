(ns ctdp.photo
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [schema.core :as s]))

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

(defn- exif-date-to-datetime
  "Exif metadata dates are strings like 2014:04:11 16:37:00.  This makes them real dates."
  [ed]
  (let [parts (str/split ed #"[ :]")]
    (assert (= (count parts) 6))
    (apply t/date-time (map #(Integer/parseInt %) parts))))

(s/defn normalise :- PhotoMetadata [metadata]
  "Return a normalised data structure for the given vendor- and photo-specific metadata"
  (let [md #(get metadata %)
        cam (camera
             {:make (md "Make")
              :model (md "Model")
              :sw (md "Software")})
        camset (camera-settings
                {:aperture (or (md "Aperture Value") "")
                 :exposure (md "Exposure Time")
                 :flash (md "Flash")
                 :focal-length (or (md "Focal Length") "")
                 :fstop (md "F-Number")
                 :iso (read-string (md "ISO Speed Ratings"))
                 :width (read-string (md "Image Width"))
                 :height (read-string (md "Image Height"))}
                )]
    (photo
     {:camera-settings camset
      :camera cam
      :datetime (exif-date-to-datetime (md "Date/Time"))
      :description (or (md "Description") "")
      :filesize (read-string (md "File Size"))})))
