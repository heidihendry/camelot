(ns camelot.util.transit
  "Transit helpers."
  (:require [cognitect.transit :as transit])
  (:import [goog.date UtcDateTime]))

(def transit-file-reader identity)

(def transit-date-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (.getTime v))
   (fn [v] (str (.getTime v)))))

(defn- transit-date-reader
  "Transit date/time reader"
  [s]
  (UtcDateTime.fromTimestamp s))

(def transit-read-handlers
  "Transit readers"
  {"m" transit-date-reader
   "f" transit-file-reader})

(def transit-write-handlers
  "Transite writers"
  {UtcDateTime transit-date-writer})

(defn request
  "Make a GET or POST request."
  [method href params]
  (method href
          (merge (or (select-keys params [:query-params]) {})
                 {:transit-params params
                  :transit-opts {:decoding-opts
                                 {:handlers transit-read-handlers}
                                 :encoding-opts
                                 {:handlers transit-write-handlers}}})))
