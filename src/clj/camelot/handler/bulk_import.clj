(ns camelot.handler.bulk-import
  "Provide high-level handling for bulk import support.  Bulk import consists
  of template generation, field mapping, validation and the actual import
  itself."
  (:require [ring.util.response :as r]
            [camelot.import.dirtree :as dt]
            [clojure.data.csv :as csv]
            [clj-time.format :as tf]
            [clj-time.local :as tl]))

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
