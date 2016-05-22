(ns camelot.util.transit
  (:require [cognitect.transit :as transit]
            [clj-time.coerce :as c]
            [camelot.util.java-file :refer [get-path]])
  (:import [org.joda.time DateTime]
           [java.io File]))

(def joda-time-reader
  "Process times from transit into Joda DateTimes."
  (transit/read-handler #(c/from-long ^java.lang.Long (Long/parseLong %))))

(def file-reader
  "Process files from transit into java Files."
  (transit/read-handler #(File. ^java.lang.Long (Long/parseLong %))))

(def joda-time-writer
  "Serialise Joda DateTime for transit."
  (transit/write-handler (constantly "m")
                         #(c/to-long %)
                         #(-> % c/to-long .toString)))

(def file-writer
  "Serialise Java Files for transit."
  (transit/write-handler (constantly "f")
                         #(identity %)
                         #(get-path %)))

(def transit-write-options
  "Transit writer options."
  {:handlers {org.joda.time.DateTime joda-time-writer
              File file-writer}})

(def transit-read-options
  "Transit reader options."
  {:handlers {"m" joda-time-reader
              "f" file-reader}})
