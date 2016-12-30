(ns camelot.import.template
  (:require [schema.core :as s]
            [clojure.string :as str]
            [clojure.data.csv :as csv]
            [clojure.tools.logging :as log]
            [clj-time.format :as tf]
            [camelot.import.dirtree :as dt]
            [camelot.import.scan-dir :as scan-dir]
            [clj-time.local :as tl]
            [camelot.util.model :as model]
            [camelot.import.datatype :as datatype]
            [clojure.edn :as edn]
            [ring.util.response :as r]
            [camelot.translation.core :as tr]
            [camelot.util.file :as file]
            [clojure.java.io :as io]))

(def time-formatter (tf/formatter-local "yyyy-MM-dd_HHmm"))

(def default-column-mappings
  {:trap-station-latitude "Camelot GPS Latitude"
   :trap-station-longitude "Camelot GPS Longitude"
   :media-capture-timestamp "Date/Time"
   :camera-make "Make"
   :camera-model "Model"
   :trap-station-altitude "GPS Altitude"
   :site-country "Country/Primary Location Name"
   :site-state-province "Province/State"
   :site-city "City"
   :site-sublocation "Sub-location"
   :photo-fnumber-setting "Aperture Value"
   :photo-exposure-value "Exposure Bias Value"
   :photo-flash-setting "Flash"
   :photo-focal-setting "Focal Length"
   :photo-iso-setting "ISO Speed Ratings"
   :photo-orientation "Orientation"
   :photo-resolution-x "Image Height"
   :photo-resolution-y "Image Width"})

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

(defn calculate-gps-latitude
  [data]
  (to-latitude (get data "GPS Latitude") (get data "GPS Latitude Ref")))

(defn calculate-gps-longitude
  [data]
  (to-longitude (get data "GPS Longitude") (get data "GPS Longitude Ref")))

(def calculated-columns
  {"Camelot GPS Longitude" calculate-gps-longitude
   "Camelot GPS Latitude" calculate-gps-latitude})
(def calculated-column-names (set (keys calculated-columns)))

(defn- to-csv-string
  "Return data as a CSV string."
  [data]
  (with-open [io-str (java.io.StringWriter.)]
    (csv/write-csv io-str data)
    (str io-str)))

(defn calculate-key-value
  [data k]
  (if-let [cfn (calculated-columns k)]
    (cfn data)
    (get data k)))

(defn all-keys
  [state dir data]
  (if-let [ks (seq (flatten (map keys data)))]
    (sort (into calculated-column-names ks))
    (cond
      (and (file/directory? (io/file dir))
           (not (file/readable? (io/file dir))))
      [(tr/translate state ::directory-not-readable dir)]

      (file/directory? (io/file dir))
      [(tr/translate state ::no-media-found dir)]

      :else
      [(tr/translate state ::directory-not-found dir)])))

(defn standardise-metadata
  [ks data]
  (map (fn [r] (reduce #(conj %1 (calculate-key-value r %2)) [] ks)) data))

(defn ->data-table
  [state dir data]
  (let [ks (all-keys state dir data)]
    (cons ks (standardise-metadata ks data))))

(defn generate-template
  [state client-dir]
  (let [resdir (scan-dir/resolve-directory state client-dir)]
    (->> resdir
         (dt/directory-metadata-collection state)
         (->data-table state resdir))))

(defn- content-disposition
  []
  (format "attachment; filename=\"bulk-import-template_%s.csv\""
          (tf/unparse time-formatter (tl/local-now))))

(defn metadata-template
  "Respond with the template as a CSV."
  [state client-dir]
  (let [data (to-csv-string (generate-template state client-dir))]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition"
                  (content-disposition)))))

(defn transpose
  [m]
  (apply map list m))

(defn mappable-fields
  []
  (remove #(-> % second :unmappable) model/schema-definitions))

(defn column-compatibility
  [[title & vs]]
  {:constraints (datatype/possible-constraints vs)
   :datatypes (datatype/possible-datatypes vs)})

(defn calculate-column-properties
  "Return a map representing the properties of each column of a vector of
  vectors."
  [data]
  (reduce #(assoc %1 (first %2) (column-compatibility (rest %2)))
                      {}
                      (transpose data)))

(defmacro cond-column->
  [testfn initexpr mapping]
  (let [m# (fn [x#] (list `(~testfn (second ~x#))
                          `(assoc (first ~x#) (second ~x#))))]
    `(cond-> ~initexpr
       ~@(mapcat m# (eval mapping)))))

(defn assign-default-mappings
  [props]
  (cond-column->
   #(get props %) {}
   default-column-mappings))

(defn column-map-options
  [state
   {:keys [tempfile :- s/Str
           content-type :- s/Str
           size :- s/Int]}]
  (cond
    (not= content-type "text/csv")
    (throw (RuntimeException. "File format must be a CSV."))

    (zero? size)
    (throw (RuntimeException. "CSV must not be empty")))

  (let [data (csv/read-csv (slurp tempfile))
        props (calculate-column-properties data)]
    {:default-mappings (assoc (assign-default-mappings props)
                              :absolute-path "Absolute Path")
     :column-properties props
     :file-data data}))
