(ns camelot.util
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cognitect.transit :as transit]
            [cljs.core.async :refer [<!]])
  (:import [goog.date UtcDateTime]))

(defn with-baseurl [path]
  (if (clojure.string/starts-with? (-> js/window (aget "location") (aget "protocol")) "http")
    (let [port (-> js/window (aget "location") (aget "port"))]
      (str
       (-> js/window (aget "location") (aget "protocol"))
       "//"
       (-> js/window (aget "location") (aget "hostname"))
       (when (not (zero? (count port)))
         (str ":" port))
       path))
    (str "http://localhost:3449" path)))

(def transit-file-reader identity)

(def transit-date-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (.getTime v))
   (fn [v] (str (.getTime v)))))

(defn- transit-date-reader
  [s]
  (UtcDateTime.fromTimestamp s))

(def transit-read-handlers
  {"m" transit-date-reader
   "f" transit-file-reader})

(def transit-write-handlers
  {UtcDateTime transit-date-writer})

(defn ls-set-item!
  "Set `key' in browser's localStorage to `val`."
  [key val]
  (.setItem (.-localStorage js/window) key (transit/write (transit/writer :json {:handlers transit-write-handlers}) val)))

(defn ls-get-item
  "Returns value of `key' from browser's localStorage."
  [key]
  (transit/read (transit/reader :json {:handlers transit-read-handlers}) (.getItem (.-localStorage js/window) key)))

(defn ls-remove-item!
  "Remove the browser's localStorage value for the given `key`"
  [key]
  (.removeItem (.-localStorage js/window) key))

(defn- request
  [method href params f]
  (go
    (let [response (<! (method href
                               {:transit-params params
                                :transit-opts {:decoding-opts
                                               {:handlers transit-read-handlers}
                                               :encoding-opts
                                               {:handlers transit-write-handlers}}}))]
      (f response))))

(def postreq (partial request http/post))

(def getreq (partial request http/get))
