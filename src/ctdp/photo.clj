(ns ctdp.photo
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [schema.core :as s]
            [ctdp.model.photo :as mp])
  (:import [ctdp.model.photo Camera CameraSettings PhotoMetadata]))

(s/defn night? :- s/Bool
  [night-start night-end hour]
  (or (> hour night-start) (< hour night-end)))

(s/defn infrared-sane? :- s/Bool
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
  [metadata]
  (let [md #(get metadata %)
        cam (mp/camera
             {:make (md "Make")
              :model (md "Model")
              :sw (md "Software")})
        camset (mp/camera-settings
                {:aperture (or (md "Aperture Value") "")
                 :exposure (md "Exposure Time")
                 :flash (md "Flash")
                 :focal-length (or (md "Focal Length") "")
                 :fstop (md "F-Number")
                 :iso (read-string (md "ISO Speed Ratings"))
                 :width (read-string (md "Image Width"))
                 :height (read-string (md "Image Height"))}
                )]
    (mp/photo
     {:camera-settings camset
      :camera cam
      :datetime (exif-date-to-datetime (md "Date/Time"))
      :description (or (md "Description") "")
      :filesize (read-string (md "File Size"))})))
