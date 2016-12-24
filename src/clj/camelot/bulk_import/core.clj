(ns camelot.bulk-import.core
  "Provide high-level handling for bulk import support.  Bulk import consists
  of template generation, field mapping, validation and the actual import
  itself."
  (:require
   [clojure.core.async :refer [>!!]]
   [ring.util.response :as r]
   [camelot.bulk-import.datatype :as datatype]
   [camelot.import.dirtree :as dt]
   [camelot.import.metadata-utils :as mutil]
   [camelot.util.model :as model]
   [camelot.util.config :as config]
   [camelot.translation.core :as tr]
   [clojure.data.csv :as csv]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [schema.core :as s]
   [clojure.string :as str]
   [clojure.edn :as edn]
   [camelot.util.trap-station :as trap]
   [camelot.util.file :as file]
   [camelot.bulk-import.validate :as validate])
  (:import
   (java.util.regex Pattern)))

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

(s/defn relative-path? :- s/Bool
  [dir :- s/Str]
  (nil? (re-find #"^(/|[A-Z]:)" dir)))

(defn detect-separator
  [path]
  (cond
    (nil? path) (file/path-separator)
    (re-find #"^[A-Z]:(?:\\|$)" path) "\\"
    (and (re-find #"\\" path) (relative-path? path)) "\\"
    :else "/"))

(defn resolve-absolute-server-directory
  "Resolve a client directory, unifying it with the configured server directory, if possible."
  [server-base-dir client-dir]
  (let [svr-sep (detect-separator server-base-dir)
        svr-path (clojure.string/split (or server-base-dir "")
                                       (re-pattern (str "\\" svr-sep)))]
    (->> client-dir
         detect-separator
         (str "\\")
         re-pattern
         (clojure.string/split client-dir)
         (drop-while #(not= % (last svr-path)))
         rest
         (apply conj svr-path)
         (str/join svr-sep))))

(defn resolve-relative-server-directory
  "Resolve a directory relative to the configured server directory, if any."
  [server-base-dir client-dir]
  (let [svr-sep (detect-separator server-base-dir)
        svr-path (clojure.string/split (or server-base-dir "")
                                       (re-pattern (str "\\" svr-sep)))]
    (->> client-dir
         detect-separator
         (str "\\")
         re-pattern
         (clojure.string/split client-dir)
         (apply conj svr-path)
         (str/join svr-sep))))

(defn strategic-directory-resolver
  "Resolve a directory, either relative to the base-dir or absolutely."
  [server-base-dir client-dir]
  (let [f (if (and (relative-path? client-dir) (not (nil? server-base-dir)))
            resolve-relative-server-directory
            resolve-absolute-server-directory)]
    (f server-base-dir client-dir)))

(defn ^String re-quote
  [^String s]
  (Pattern/quote ^String s))

(defn resolve-server-directory
  "Resolve the directory, defaulting to the root path should the client attempt to escape it."
  [server-base-dir client-dir]
  (if server-base-dir
    (let [f-can (file/canonical-path (file/->file (strategic-directory-resolver server-base-dir client-dir)))
          s-can (file/canonical-path (file/->file server-base-dir))]
      (if (re-find (re-pattern (str "^" (re-quote s-can))) f-can)
        f-can
        (do
          (prn s-can)
          (prn f-can)
          s-can)))
    client-dir))

(defn resolve-directory
  "Resolve a corresponding server directory for a given 'client' directory."
  [state client-dir]
  {:pre [(not (nil? client-dir))]}
  (let [root (config/lookup state :root-path)
        res (resolve-server-directory root client-dir)]
    (cond
      (and (empty? res) (nil? root)) client-dir
      (empty? res) root
      :else res)))

(defn calculate-gps-latitude
  [data]
  (mutil/to-latitude (get data "GPS Latitude") (get data "GPS Latitude Ref")))

(defn calculate-gps-longitude
  [data]
  (mutil/to-longitude (get data "GPS Longitude") (get data "GPS Longitude Ref")))

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
  [data]
  (sort (into calculated-column-names (flatten (map keys data)))))

(defn standardise-metadata
  [ks data]
  (map (fn [r] (reduce #(conj %1 (calculate-key-value r %2)) [] ks)) data))

(defn ->data-table
  [data]
  (let [ks (all-keys data)]
    (cons ks (standardise-metadata ks data))))

(defn generate-template
  [state client-dir]
  (->> (resolve-directory state client-dir)
       (dt/directory-metadata-collection state)
       ->data-table))

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

(defn file-data-to-record-list
  "Return a vector of maps, where each map contains all data for a record."
  [state file-data headings mappings]
  (map
   (fn [row]
     (reduce-kv (fn [acc k v]
                  (if (nil? v)
                    acc
                    (let [d (nth row (get headings v))]
                      (assoc acc k (datatype/deserialise k d))))) {} mappings))
   file-data))

(defn validate-and-import
  "Validate a seq of record, and if valid, queue for import."
  [state records]
  (let [problems (validate/validate state records)]
    (if (seq problems)
      (map :reason problems)
      (do
        (>!! (get-in state [:importer :cmd-chan])
             {:state state :cmd :new})
        (doseq [r (sort-by :media-capture-timestamp records)]
          (>!! (get-in state [:importer :queue-chan])
               {:state state :record r}))))))

(defn import-with-mappings
  "Given file data and a series of mappings, attempt to import it."
  [state {:keys [file-data mappings survey-id]}]
  (let [props (calculate-column-properties file-data)
        errs (model/check-mapping mappings props (partial tr/translate state))
        headings (reduce-kv #(assoc %1 %3 %2) {} (first file-data))]
    (if (seq errs)
      {:error {:validation errs}}
      (->> mappings
           (file-data-to-record-list state (rest file-data) headings)
           (map #(merge {:survey-id survey-id} %))
           (validate-and-import state)))))
