(ns camelot.rest
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [om.core :as om])
  (:require [camelot.util :as util]
            [cljs-http.client :as http]
            [om.core :as om]
            [camelot.state :as state]
            [cljs.core.async :refer [<!]]))

(def success-status-codes
  #{200 201 202 203 204 205 206 207 300 301 302 303 307})

(defn build-error
  ([method url response]
   (str response
        "\n--------\n"
        "Requested: " url " via " method))
  ([method url params response]
   (str response
        "\n--------\n"
        "Requested: " url " via " method "\n"
        "With parameters: " params "\n")))

(defn get-x
  "Retrieve settings"
  [x-url cb]
  (go
    (let [response (<! (util/request
                        http/get (util/with-baseurl x-url) nil))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "GET"
                                                     (util/with-baseurl x-url)
                                                     (:body response)))))))

(def get-application
  "Retrieve global application details"
  (partial get-x "/settings/application"))

(def get-albums
  "Retrieve albums"
  (partial get-x "/albums"))

(def get-screens
  "Retrieve screens"
  (partial get-x "/screens"))

(def get-configuration
  "Retrieve configuration"
  (partial get-x "/settings"))

(def get-metadata
  "Retrieve metadata"
  (partial get-x "/settings/metadata"))

(defn get-resource
  "GET resource state"
  [resource cb]
  (go
    (let [response (<! (util/request http/get (util/with-baseurl resource)
                                     nil))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "GET"
                                                     (util/with-baseurl resource)
                                                     (:body response)))))))

(defn put-resource
  "PUT resource state"
  [resource params cb]
  (go
    (let [response (<! (util/request http/put (util/with-baseurl resource)
                                     params))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "PUT"
                                                     (util/with-baseurl resource)
                                                     params
                                                     (:body response)))))))

(defn post-resource
  "POST resource state"
  [resource params cb]
  (go
    (let [response (<! (util/request http/post (util/with-baseurl resource)
                                     params))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "POST"
                                                     (util/with-baseurl resource)
                                                     params
                                                     (:body response)))))))

(defn delete-resource
  "DELETE resource"
  [resource params cb]
  (go
    (let [response (<! (util/request http/delete (util/with-baseurl resource)
                                     params))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "DELETE"
                                                     (util/with-baseurl resource)
                                                     params
                                                     (:body response)))))))

(defn post-settings
  "POST configuration state"
  [params cb]
  (go
    (let [response (<! (util/request http/post (util/with-baseurl "/settings")
                                     params))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "POST"
                                                     (util/with-baseurl "/settings")
                                                     params
                                                     (:body response)))))))
