(ns camelot.util
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cognitect.transit :as transit])
  (:import [goog.date DateTime]))

(defn with-baseurl
  "Return the given path along with the correct base URL."
  [path]
  (let [protocol (-> js/window (aget "location") (aget "protocol"))
        port (-> js/window (aget "location") (aget "port"))]
    (if (or (clojure.string/starts-with? protocol "http")
            (clojure.string/starts-with? protocol "https"))
      (str
       (-> js/window (aget "location") (aget "protocol"))
       "//"
       (-> js/window (aget "location") (aget "hostname"))
       (when-not (zero? (count port))
         (str ":" port))
       path)
      (str "http://localhost:3449" path))))

(def transit-file-reader identity)

(def transit-date-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (.getTime v))
   (fn [v] (str (.getTime v)))))

(defn- transit-date-reader
  "Transit date/time reader"
  [s]
  (DateTime.fromTimestamp s))

(def transit-read-handlers
  "Transit readers"
  {"m" transit-date-reader
   "f" transit-file-reader})

(def transit-write-handlers
  "Transite writers"
  {DateTime transit-date-writer})

(defn- request
  "Make a GET or POST request."
  [method href params]
  (method href
          {:transit-params params
           :transit-opts {:decoding-opts
                          {:handlers transit-read-handlers}
                          :encoding-opts
                          {:handlers transit-write-handlers}}}))
