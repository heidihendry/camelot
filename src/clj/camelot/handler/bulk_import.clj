(ns camelot.handler.bulk-import
  "Provide high-level handling for bulk import support.  Bulk import consists
  of template generation, field mapping, validation and the actual import
  itself."
  (:require [ring.util.response :as r]
            [camelot.import.dirtree :as dt]
            [camelot.util.model :as model]
            [clojure.data.csv :as csv]
            [clj-time.format :as tf]
            [clj-time.local :as tl]
            [schema.core :as s]))

(def time-formatter (tf/formatter-local "yyyy-MM-dd_HHmm"))

(defn- to-csv-string
  "Return data as a CSV string."
  [data]
  (with-open [io-str (java.io.StringWriter.)]
    (csv/write-csv io-str data)
    (.toString io-str)))

(defn all-keys
  [data]
  (sort (into #{} (flatten (map keys data)))))

(defn standardise-metadata
  [ks data]
  (map (fn [r] (reduce #(conj %1 (get r %2)) [] ks)) data))

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

(defn associate-options
  [xs]
  model/fields)

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
    (reduce #(assoc %1 (first %2) (associate-options (rest %2)))
            {}
            (transpose data))))
