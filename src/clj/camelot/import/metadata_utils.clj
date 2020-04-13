(ns camelot.import.metadata-utils
  (:require
   [clojure.string :as str]
   [clj-time.core :as t]
   [clojure.edn :as edn]
   [schema.core :as sch]
   [clojure.tools.logging :as log]))

(sch/defrecord ImportPhotoMetadata
    [datetime :- (sch/maybe org.joda.time.DateTime)
     photo-exposure-value :- (sch/maybe sch/Str)
     photo-flash-setting :- (sch/maybe sch/Str)
     photo-focal-length :- (sch/maybe sch/Str)
     photo-fnumber-setting :- (sch/maybe sch/Str)
     photo-iso-setting :- (sch/maybe sch/Num)
     photo-orientation :- (sch/maybe sch/Str)
     photo-resolution-x :- (sch/maybe sch/Num)
     photo-resolution-y :- (sch/maybe sch/Num)])

(sch/defn exif-date-to-datetime :- org.joda.time.DateTime
  "Exif metadata dates are strings like 2014:04:11 16:37:00.  This makes them real dates.
Important: Timezone information will be discarded."
  [ed :- sch/Str]
  (let [parts (str/split (first (str/split ed #"\+")) #"[ :]")]
    (assert (= (count parts) 6))
    (apply t/date-time (map #(Integer/parseInt %) parts))))

(defn read-metadata-string
  "Return str as a number, or zero if nil."
  [str]
  (when str
    (try (edn/read-string str)
         (catch java.lang.Exception _
           (log/warn "read-metadata-string: Attempt to read-string on '" str "'")
           nil))))

(sch/defn valid-raw-data?
  "Check the minimum required fields are present in the metadata, returning an
invalid entry if not."
  [state raw-metadata]
  (if (or (get raw-metadata "Date/Time")
          ;; Some Reconyx cameras set only this.
          (get raw-metadata "Date/Time Original"))
    true
    false))

(sch/defn normalise :- ImportPhotoMetadata
  "Return a normalised data structure for the given vendor- and photo-specific metadata"
  [state raw-metadata]
  (let [md #(get raw-metadata %)]
    (map->ImportPhotoMetadata
     {:datetime (exif-date-to-datetime (or (md "Date/Time")
                                           (md "Date/Time Original")))
      :photo-exposure-value (md "Exposure Time")
      :photo-flash-setting (md "Flash")
      :photo-focal-length (md "Focal Length")
      :photo-fnumber-setting (md "F-Number")
      :photo-iso-setting (read-metadata-string (md "ISO Speed Ratings"))
      :photo-orientation (md "Orientation")
      :photo-resolution-x (read-metadata-string (md "Image Width"))
      :photo-resolution-y (read-metadata-string (md "Image Height"))})))

(sch/defn parse :- (sch/maybe ImportPhotoMetadata)
  "Validate a photo's raw metadata and normalise, if possible."
  [state raw-metadata]
  (when (valid-raw-data? state raw-metadata)
    (normalise state raw-metadata)))
