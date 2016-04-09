(ns camelot.rest
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [camelot.util :as util]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn get-application
  "Retrieve global application details"
  [cb]
  (go
    (let [response (<! (util/request
                        http/get (util/with-baseurl "/application")
                        nil))]
      (cb response))))

(defn get-albums
  "Retrieve albums"
  [cb]
  (go
    (let [response (<! (util/request
                        http/get (util/with-baseurl "/albums")
                        nil))]
      (cb response))))

(defn get-settings
  "Retrieve settings"
  [cb]
  (go
    (let [response (<! (util/request
                        http/get (util/with-baseurl "/settings") nil))]
      (cb response))))

(defn get-configuration
  "Retrieve configuration"
  [cb]
  (go
    (let [response (<! (util/request
                        http/get (util/with-baseurl "/default-config") nil))]
      (cb response))))

(defn post-settings
  "POST configuration state"
  [params cb]
  (go
    (let [response (<! (util/request http/post (util/with-baseurl "/settings")
                                     params))]
      (cb response))))
