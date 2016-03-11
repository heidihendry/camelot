(ns ctdp.photo-processor
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [schema.core :as s])
  (:import [com.drew.imaging ImageMetadataReader]))

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
    [make :- s/Keyword
     model :- s/Str
     software :- s/Str])

(s/defrecord PhotoMetadata
    [datetime :- org.joda.time.DateTime
     description :- s/Str
     filesize :- s/Num
     camera :- Camera
     settings :- CameraSettings])

(defn- get-vendor [metadata]
  (cond
    (= (str/lower-case (get metadata "Make")) "maginon") :maginon
    (= (str/lower-case (get metadata "Make")) "cuddeback") :cuddeback))

(s/defn camera :- Camera
  "Camera constructor"
  [{:keys [make model sw]}]
  {:pre [(keyword? make)
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

(defn- md-to-datetime
  "Exif metadata dates are strings like 2014:04:11 16:37:00.  This makes them real dates."
  [mddate]
  (let [parts (str/split mddate #"[ :]")]
    (assert (= (count parts) 6))
    (apply t/date-time (map #(Integer/parseInt %) parts))))

(defmulti is-infrared
  "Normalise vendor metadata"
  :make)
(s/defmethod is-infrared :cuddeback :- s/Bool [metadata])

(defmulti vendor
  "Normalise vendor metadata"
  get-vendor)
(s/defmethod vendor :maginon :- PhotoMetadata [metadata]
  (let [md #(get metadata %)
        cam (camera
             {:make :maginon
              :model (md "Model")
              :sw (md "Software")})
        camset (camera-settings
                {:aperture (md "Aperture Value")
                 :exposure (md "Exposure Time")
                 :flash (md "Flash")
                 :focal-length (md "Focal Length")
                 :fstop (md "F-Number")
                 :iso (read-string (md "ISO Speed Ratings"))
                 :width (read-string (md "Image Width"))
                 :height (read-string (md "Image Height"))}
                )]
    (photo
     {:camera-settings camset
      :camera cam
      :datetime (md-to-datetime (md "Date/Time"))
      :description (or (md "Description") "")
      :filesize (read-string (md "File Size"))})))

(s/defmethod vendor :cuddeback :- PhotoMetadata [metadata]
  (let [md #(get metadata %)
        cam (camera
             {:make :cuddeback
              :model (md "Model")
              :sw (md "Software")})
        camset (camera-settings
                {:aperture ""
                 :exposure (md "Exposure Time")
                 :flash (md "Flash")
                 :focal-length ""
                 :fstop (md "F-Number")
                 :iso (read-string (md "ISO Speed Ratings"))
                 :width (read-string (md "Image Width"))
                 :height (read-string (md "Image Height"))}
                )]
    (photo
     {:camera-settings camset
      :camera cam
      :datetime (md-to-datetime (md "Date/Time"))
      :description (or (md "Description") "")
      :filesize (read-string (md "File Size"))})))

(defn- extract-from-tag
  [tag]
  (into {} (map #(hash-map (.getTagName %) (-> % (.getDescription) (str/trim))) tag)))

(defn file-metadata
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map"
  [file]
  (let [metadata (ImageMetadataReader/readMetadata file)
        tags (map #(.getTags %) (.getDirectories metadata))]
    (vendor (into {} (map extract-from-tag tags)))))
