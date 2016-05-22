(ns camelot.processing.metadata-utils
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [clojure.edn :as edn]
            [schema.core :as s]
            [camelot.model.photo :as mp]
            [clojure.tools.logging :as log])
  (:import [camelot.model.photo Camera CameraSettings PhotoMetadata]))

(s/defn gps-parts-to-decimal :- s/Num
  "Return the GPS parts as a decimal."
  [parts :- [s/Num]]
  {:pre [(= (count parts) 3)]}
  (let [exact (+ (first parts)
                 (/ (/ (nth parts 1) 0.6) 100)
                 (/ (/ (nth parts 2) 0.36) 10000))]
    (edn/read-string (format "%.6f" exact))))

(s/defn gps-degrees-as-parts
  "Return the numeric parts of a GPS location as a vector, given a string in degrees."
  [deg]
  (->> (str/split deg #" ")
       (map #(str/replace % #"[^\.0-9]" ""))
       (mapv edn/read-string)))

(s/defn parse-gps :- s/Num
  "Convert degrees string with a reference to a decimal.
`pos-ref' is the reference direction which is positive; any other
direction is considered negative."
  [pos-ref :- s/Str
   mag :- s/Str
   mag-ref :- s/Str]
  (let [decimal (-> mag
                    (gps-degrees-as-parts)
                    (gps-parts-to-decimal))]
    (if (= mag-ref pos-ref)
      decimal
      (* -1 decimal))))

(s/defn to-longitude :- (s/maybe s/Num)
  "Convert longitude in degrees and a longitude reference to a decimal."
  [lon :- (s/maybe s/Str)
   lon-ref :- (s/maybe s/Str)]
  (when-not (or (nil? lon) (nil? lon-ref))
    (try (parse-gps "E" lon lon-ref)
         (catch java.lang.Exception e
           (do
             (log/warn "to-longitude: Attempt to parse " lon " as GPS")
             nil)))))

(s/defn to-latitude :- (s/maybe s/Num)
  "Convert latitude in degrees and a latitude reference to a decimal."
  [lat :- (s/maybe s/Str)
   lat-ref :- (s/maybe s/Str)]
  (when-not (or (nil? lat) (nil? lat-ref))
    (try (parse-gps "N" lat lat-ref)
         (catch java.lang.Exception e
           (do
             (log/warn "to-latitude: Attempt to parse " lat " as GPS")
             nil)))))

(s/defn exif-date-to-datetime :- org.joda.time.DateTime
  "Exif metadata dates are strings like 2014:04:11 16:37:00.  This makes them real dates.
Important: Timezone information will be discarded."
  [ed :- s/Str]
  (let [parts (str/split (first (str/split ed #"\+")) #"[ :]")]
    (assert (= (count parts) 6))
    (apply t/date-time (map #(Integer/parseInt %) parts))))

(defn read-metadata-string
  "Return str as a number, or zero if nil."
  [str default]
  (if str
    (try (edn/read-string str)
         (catch java.lang.Exception e
           (do
             (log/warn "read-metadata-string: Attempt to read-string on '" str "'")
             default)))
    default))

(s/defn exif-gps-datetime
  "Concatenate a date and time, returning the result as a DateTime."
  [date time]
  (when (and (string? date) (string? time))
    (exif-date-to-datetime (str date " " (first (str/split time #"\."))))))

(s/defn validate-raw-data
  "Check the minimum required fields are present in the metadata, returning an
invalid entry if not."
  [state raw-metadata]
  (let [strictly-required-fields ["Date/Time" "File Name"]
        missing (remove #(get raw-metadata %) strictly-required-fields)]
    (when-not (empty? missing)
      (str/join ", " missing))))

(s/defn normalise :- PhotoMetadata
  "Return a normalised data structure for the given vendor- and photo-specific metadata"
  [state raw-metadata]
  (let [md #(get raw-metadata %)
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
                 :iso (read-metadata-string (md "ISO Speed Ratings") 0)
                 :orientation (md "Orientation")
                 :width (read-metadata-string (md "Image Width") 0)
                 :height (read-metadata-string (md "Image Height") 0)})
        location (mp/location
                  {:gps-longitude (to-longitude (md "GPS Longitude") (md "GPS Longitude Ref"))
                   :gps-latitude (to-latitude (md "GPS Latitude") (md "GPS Latitude Ref"))
                   :gps-altitude (md "GPS Altitude")
                   :sublocation (md "Sub-location")
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
                                  :quantity (read-metadata-string (md "Object Name") nil)})]
                   [])
      :datetime (exif-date-to-datetime (md "Date/Time"))
      :datetime-original (exif-gps-datetime (md "GPS Date Stamp")
                                            (md "GPS Time-Stamp"))
      :headline (md "Headline")
      :artist (md "Artist")
      :phase (md "Source")
      :copyright (md "Copyright Notice")
      :description (md "Description")
      :filename (md "File Name")
      :filesize (read-metadata-string (md "File Size") 0)
      :location location})))
