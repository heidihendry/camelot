(ns camelot.handler.bulk-import
  "Provide high-level handling for bulk import support.  Bulk import consists
  of template generation, field mapping, validation and the actual import
  itself."
  (:require
   [ring.util.response :as r]
   [camelot.import.dirtree :as dt]
   [camelot.util.model :as model]
   [camelot.util.config :as config]
   [clojure.data.csv :as csv]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [camelot.import.metadata-utils :as mutil]
   [schema.core :as s]
   [clojure.string :as str]
   [clojure.edn :as edn]
   [camelot.util.trap-station :as trap]
   [clojure.java.io :as io]
   [camelot.util.java-file :as jf]))

(def time-formatter (tf/formatter-local "yyyy-MM-dd_HHmm"))

(defn detect-separator
  [path]
  (cond
    (nil? path) (config/get-directory-separator)
    (re-find #"^[A-Z]:(?:\\|$)" path) "\\"
    :else "/"))

(defn resolve-server-directory
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
         (drop-while #(not= % (last svr-path)))
         rest
         (apply conj svr-path)
         (str/join svr-sep))))

(defn resolve-directory
  "Resolve a corresponding server directory for a given 'client' directory."
  [state client-dir]
  {:pre (nil? client-dir)}
  (let [root (-> state :config :root-path)
        res (resolve-server-directory root client-dir)]
    (io/file (if (empty? res)
               client-dir
               res))))

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
  [state]
  (->> (get-in state [:config :root-path])
       (dt/directory-metadata-collection state)
       ->data-table))

(defn- content-disposition
  []
  (format "attachment; filename=\"bulk-import-template_%s.csv\""
          (tf/unparse time-formatter (tl/local-now))))

(defn metadata-template
  "Respond with the template as a CSV."
  [state]
  (let [data (to-csv-string (generate-template state))]
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
  (let [f (io/file x)]
    (and (jf/exists? f)
         (jf/readable? f)
         (jf/file? f))))

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

  (let [data (csv/read-csv (slurp tempfile))]
    (reduce #(assoc %1 (first %2) (column-compatibility (rest %2)))
            {}
            (transpose data))))
