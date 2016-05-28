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

(defn- -build-error
  [method url params status response]
  (let [sep "\n--------\n"
        req (str "Requested: " url " via " method)
        stat (str "Status Code: " status "\n")]
    (cond
      (zero? status) (str "Unable to contact server" sep req)
      (nil? params) (str response sep stat req)
      :else (str response sep stat req
                 "\nWith parameters: " params "\n"))))

(defn build-error
  ([method url status response]
   (-build-error method url nil status response))
  ([method url params status response]
   (-build-error method url params status response)))

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
                                                     (:status response)
                                                     (:body response)))))))

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
  (partial get-x "/settings"))

(def get-metadata
  "Retrieve metadata"
  (partial get-x "/application/metadata"))

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
                                                     (:status response)
                                                     (:body response)))))))

(defn post-import-state
  "POST import state"
  [params cb]
  (go
    (let [response (<! (util/request http/post (util/with-baseurl "/import/options")
                                     params))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "GET"
                                                     (util/with-baseurl "/import/options")
                                                     (:status response)
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
                                                     (:status response)
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
                                                     (:status response)
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
                                                     (:status response)
                                                     (:body response)))))))
