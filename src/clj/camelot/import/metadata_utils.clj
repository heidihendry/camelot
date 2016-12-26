(ns camelot.import.metadata-utils
  (:require
   [clojure.string :as str]
   [clj-time.core :as t]
   [clojure.edn :as edn]
   [schema.core :as s]
   [clojure.tools.logging :as log]))

(s/defrecord ImportPhotoMetadata
    [datetime :- (s/maybe org.joda.time.DateTime)
     photo-exposure-value :- (s/maybe s/Str)
     photo-flash-setting :- (s/maybe s/Str)
     photo-focal-length :- (s/maybe s/Str)
     photo-fnumber-setting :- (s/maybe s/Str)
     photo-iso-setting :- (s/maybe s/Num)
     photo-orientation :- (s/maybe s/Str)
     photo-resolution-x :- (s/maybe s/Num)
     photo-resolution-y :- (s/maybe s/Num)])

(s/defn exif-date-to-datetime :- org.joda.time.DateTime
  "Exif metadata dates are strings like 2014:04:11 16:37:00.  This makes them real dates.
Important: Timezone information will be discarded."
  [ed :- s/Str]
  (let [parts (str/split (first (str/split ed #"\+")) #"[ :]")]
    (assert (= (count parts) 6))
    (apply t/date-time (map #(Integer/parseInt %) parts))))

(defn read-metadata-string
  "Return str as a number, or zero if nil."
  [str]
  (when str
    (try (edn/read-string str)
         (catch java.lang.Exception e
           (do
             (log/warn "read-metadata-string: Attempt to read-string on '" str "'")
             nil)))))

(s/defn valid-raw-data?
  "Check the minimum required fields are present in the metadata, returning an
invalid entry if not."
  [state raw-metadata]
  (if (get raw-metadata "Date/Time")
    true
    false))

(s/defn normalise :- ImportPhotoMetadata
  "Return a normalised data structure for the given vendor- and photo-specific metadata"
  [state raw-metadata]
  (let [md #(get raw-metadata %)]
    (map->ImportPhotoMetadata
     {:datetime (exif-date-to-datetime (md "Date/Time"))
      :photo-exposure-value (md "Exposure Time")
      :photo-flash-setting (md "Flash")
      :photo-focal-length (md "Focal Length")
      :photo-fnumber-setting (md "F-Number")
      :photo-iso-setting (read-metadata-string (md "ISO Speed Ratings"))
      :photo-orientation (md "Orientation")
      :photo-resolution-x (read-metadata-string (md "Image Width"))
      :photo-resolution-y (read-metadata-string (md "Image Height"))})))

(s/defn parse :- (s/maybe ImportPhotoMetadata)
  "Validate a photo's raw metadata and normalise, if possible."
  [state raw-metadata]
  (when (valid-raw-data? state raw-metadata)
    (normalise state raw-metadata)))
