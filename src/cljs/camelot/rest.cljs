(ns camelot.rest
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [camelot.util :as util]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn get-x
  "Retrieve settings"
  [x-url cb]
  (go
    (let [response (<! (util/request
                        http/get (util/with-baseurl x-url) nil))]
      (cb response))))

(def get-application
  "Retrieve global application details"
  (partial get-x "/application"))

(def get-albums
  "Retrieve albums"
  (partial get-x "/albums"))

(def get-screens
  "Retrieve screens"
  (partial get-x "/screens"))

(def get-configuration
  "Retrieve configuration"
  (partial get-x "/default-config"))

(def get-metadata
  "Retrieve metadata"
  (partial get-x "/metadata"))

(def get-surveys
  "Retrieve configuration"
  (partial get-x "/surveys"))

(defn post-settings
  "POST configuration state"
  [params cb]
  (go
    (let [response (<! (util/request http/post (util/with-baseurl "/settings")
                                     params))]
      (cb response))))

