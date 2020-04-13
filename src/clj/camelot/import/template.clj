(ns camelot.import.template
  (:require
   [camelot.import.dirtree :as dt]
   [camelot.import.scan-dir :as scan-dir]
   [camelot.model.trap-station :as trap-station]
   [camelot.translation.core :as tr]
   [camelot.util.datatype :as datatype]
   [camelot.util.file :as file]
   [camelot.util.model :as model]
   [camelot.util.bulk-import :refer [default-column-mappings]]
   [clojure.data.csv :as csv]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [schema.core :as sch]))

(sch/defn gps-parts-to-decimal :- sch/Num
  "Return the GPS parts as a decimal."
  [parts :- [sch/Num]]
  {:pre [(= (count parts) 3)]}
  (let [exact (+ (first parts)
                 (/ (nth parts 1) 0.6 100)
                 (/ (nth parts 2) 0.36 10000))]
    (trap-station/round-gps exact)))

(sch/defn gps-degrees-as-parts
  "Return the numeric parts of a GPS location as a vector, given a string in degrees."
  [deg]
  (->> (str/split deg #" ")
       (map #(str/replace % #"[^\.0-9]" ""))
       (mapv edn/read-string)))

(sch/defn parse-gps :- sch/Num
  "Convert degrees string with a reference to a decimal.
`pos-ref' is the reference direction which is positive; any other
direction is considered negative."
  [pos-ref :- sch/Str
   mag :- sch/Str
   mag-ref :- sch/Str]
  (let [decimal (-> mag
                    gps-degrees-as-parts
                    gps-parts-to-decimal)]
    (if (= mag-ref pos-ref)
      decimal
      (* -1 decimal))))

(sch/defn to-longitude :- (sch/maybe sch/Num)
  "Convert longitude in degrees and a longitude reference to a decimal."
  [lon :- (sch/maybe sch/Str)
   lon-ref :- (sch/maybe sch/Str)]
  (when-not (or (nil? lon) (nil? lon-ref))
    (try (parse-gps "E" lon lon-ref)
         (catch java.lang.Exception _
           (log/warn "to-longitude: Attempt to parse " lon " as GPS")
           nil))))

(sch/defn to-latitude :- (sch/maybe sch/Num)
  "Convert latitude in degrees and a latitude reference to a decimal."
  [lat :- (sch/maybe sch/Str)
   lat-ref :- (sch/maybe sch/Str)]
  (when-not (or (nil? lat) (nil? lat-ref))
    (try (parse-gps "N" lat lat-ref)
         (catch java.lang.Exception _
           (log/warn "to-latitude: Attempt to parse " lat " as GPS")
           nil))))

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
         (->data-table state resdir)
         to-csv-string)))

(defn transpose
  [m]
  (apply map list m))

(defn mappable-fields
  []
  (remove #(-> % second :unmappable) model/schema-definitions))

(defn column-compatibility
  [vs]
  {:constraints (datatype/possible-constraints vs)
   :max-length (datatype/max-length vs)
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
  [state {:keys [tempfile content-type size]}]
  (let [filedata (slurp tempfile)]
    (cond
      ;; Assume a non-empty file without a null-byte can be treated as a CSV.
      (some zero? (map int filedata))
      (throw (RuntimeException. "File format must be a CSV."))

      (zero? size)
      (throw (RuntimeException. "CSV must not be empty")))


    (let [csvdata (csv/read-csv filedata)
          props (calculate-column-properties csvdata)]
      {:default-mappings (assoc (assign-default-mappings props)
                                :absolute-path "Absolute Path")
       :column-properties props
       :file-data csvdata})))
