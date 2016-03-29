(ns camelot.util
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cognitect.transit :as transit]
            [cljs.core.async :refer [<!]])
  (:import [goog.date UtcDateTime]))

(defn ls-set-item!
  "Set `key' in browser's localStorage to `val`."
  [key val]
  (.setItem (.-localStorage js/window) key (transit/write (transit/writer :json) val)))

(defn ls-get-item
  "Returns value of `key' from browser's localStorage."
  [key]
  (transit/read (transit/reader :json) (.getItem (.-localStorage js/window) key)))

(defn ls-remove-item!
  "Remove the browser's localStorage value for the given `key`"
  [key]
  (.removeItem (.-localStorage js/window) key))

(def transit-date-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (.getTime v))
   (fn [v] (str (.getTime v)))))

(defn- transit-date-reader
  [s]
  (UtcDateTime.fromTimestamp s))

(def transit-file-reader identity)

(defn- request
  [method href params f]
  (go
    (let [response (<! (method href
                               {:transit-params params
                                :transit-opts {:decoding-opts
                                               {:handlers {"m" transit-date-reader
                                                           "f" transit-file-reader}}
                                               :encoding-opts
                                               {:handlers {UtcDateTime transit-date-writer}}}}))]
      (f response))))

(def postreq (partial request http/post))

(def getreq (partial request http/get))
