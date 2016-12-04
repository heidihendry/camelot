(ns camelot.bulk-import.core
  "Provide high-level handling for bulk import support.  Bulk import consists
  of template generation, field mapping, validation and the actual import
  itself."
  (:require
   [ring.util.response :as r]
   [camelot.import.dirtree :as dt]
   [camelot.import.metadata-utils :as mutil]
   [camelot.util.model :as model]
   [camelot.app.state :as state]
   [clojure.data.csv :as csv]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [schema.core :as s]
   [clojure.string :as str]
   [clojure.edn :as edn]
   [camelot.util.trap-station :as trap]
   [camelot.util.file :as file])
  (:import
   (java.util.regex Pattern)))

(def time-formatter (tf/formatter-local "yyyy-MM-dd_HHmm"))

(s/defn relative-path? :- s/Bool
  [dir :- s/Str]
  (nil? (re-find #"^(/|[A-Z]:)" dir)))

(defn detect-separator
  [path]
  (cond
    (nil? path) (state/get-directory-separator)
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
  {:pre (nil? client-dir)}
  (let [root (-> state :config :root-path)
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
    (.toString io-str)))

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

;; must support

;; 2014/02/01 01:09:02
;; 2014/02/01 01:09:02
;; 2014/2/1 1:09:02
;; 2014:04:27 06:56:02
;; Thu Apr 21 21:57:45 +10:00 2016

(defn try-parse
  [fmt x]
  (try
    (tf/parse fmt x)
    (catch Exception _ nil)))

(defn could-be-timestamp?
  [x]
  (or (empty? x)
      (try-parse (tf/formatter "yyyy-M-d H:m:s") x)
      (try-parse (tf/formatter "yyyy/M/d H:m:s") x)
      (try-parse (tf/formatter "E MMM d H:m:s Z yyyy") x)
      (try-parse (tf/formatter "yyyy:M:d H:m:s") x)
      (tf/parse x)))

(defn could-be-number?
  [x]
  (or (empty? x)
      (seq (re-matches #"^-?[0-9]+(\.[0-9]+)?$" x))))

(defn could-be-integer?
  [x]
  (or (empty? x)
      (seq (re-matches #"^-?[0-9]+$" x))))

(defn could-be-yes-no?
  [x]
  (or (empty? x)
      (seq (re-matches #"^(?i)y(es)?|n(o)?$" x))))

(defn could-be-zero-one?
  [x]
  (or (empty? x)
      (seq (re-matches #"0|1" x))))

(defn could-be-true-false?
  [x]
  (or (empty? x)
      (seq (re-matches #"^(?i)T(rue)?|F(alse)?$" x))))

(defn could-be-longitude?
  [x]
  (try
    (trap/valid-longitude? (Long/parseLong x))
    (catch Exception _ nil)))

(defn could-be-latitude?
  [x]
  (try
    (trap/valid-latitude? (Long/parseLong x))
    (catch Exception _ nil)))

(defn could-be-sex?
  [x]
  (or (empty? x)
      (seq (re-matches #"^(?i)M(ale)?|F(emale)?$" x))))

(defn could-be-lifestage?
  [x]
  (or (empty? x)
      (seq (re-matches #"^(?i)A(dult)?|J(uvenile)?$" x))))

(defn could-be-file?
  [x]
  (let [f (file/->file x)]
    (and (file/exists? f)
         (file/readable? f)
         (file/file? f))))

(defn could-be-required?
  [x]
  (not (empty? x)))

(defn check-possible
  [token checkfn xs]
  (when (every? checkfn xs)
    token))

(defn possible-datatypes
  [xs]
  (disj (set
         [(check-possible :timestamp could-be-timestamp? xs)
          (check-possible :number could-be-number? xs)
          (check-possible :integer could-be-integer? xs)
          (check-possible :boolean could-be-yes-no? xs)
          (check-possible :boolean could-be-zero-one? xs)
          (check-possible :boolean could-be-true-false? xs)
          (check-possible :latitude could-be-latitude? xs)
          (check-possible :longitude could-be-longitude? xs)
          (check-possible :sex could-be-sex? xs)
          (check-possible :lifestage could-be-lifestage? xs)
          (check-possible :file could-be-file? xs)
          :string])
        nil))

(defn possible-constraints
  [xs]
  (disj (set
         [(check-possible :required could-be-required? xs)])
        nil))

(defn mappable-fields
  []
  (remove #(-> % second :unmappable) model/schema-definitions))

(defn column-compatibility
  [[title & vs]]
  {:constraints (possible-constraints vs)
   :datatypes (possible-datatypes vs)})

(defmacro cond-column->
  [testfn initexpr mapping]
  (let [m# (fn [x#] (list `(~testfn (second ~x#))
                          `(assoc (first ~x#) (second ~x#))))]
    `(cond-> ~initexpr
       ~@(mapcat m# mapping))))

(defn assign-default-mappings
  [props]
  (cond-column->
   #(get props %) {}
   {:trap-station-latitude "Camelot GPS Latitude"
    :trap-station-longitude "Camelot GPS Longitude"
    :media-capture-timestamp "Date/Time"
    :camera-make "Make"
    :camera-model "Model"
    :trap-station-altitude "GPS Altitude"
    :site-country "Country/Primary Location Name"
    :site-state-province "Province/State"
    :site-city "City"
    :site-sublocation "Sub-location"}))

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
        props (reduce #(assoc %1 (first %2) (column-compatibility (rest %2)))
                      {}
                      (transpose data))]
    {:default-mappings (assoc (assign-default-mappings props)
                              :absolute-path "Absolute Path")
     :column-properties props
     :file-data data}))

(defn import-with-mappings
  [session data]
  data)
