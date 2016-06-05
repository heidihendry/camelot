(ns camelot.report.util
  (:require [clojure.data.csv :as csv]
            [schema.core :as s]))

(s/defn to-csv-string :- s/Str
  "Return data as a CSV string."
  [data]
  (with-open [io-str (java.io.StringWriter.)]
    (csv/write-csv io-str data)
    (.toString io-str)))
