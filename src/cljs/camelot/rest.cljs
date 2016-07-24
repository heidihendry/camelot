(ns camelot.rest
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [om.core :as om])
  (:require [camelot.util.transit :as transit-util]
            [camelot.util.misc :as misc]
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

(defn get-x-raw
  "Send an normal (transit-free) GET request."
  ([x-url params cb]
   (go
     (let [response (<! (http/get (misc/with-baseurl x-url) params))
           success (some #{(:status response)} success-status-codes)]
       (if success
         (when cb
           (cb response))
         (om/update! (state/app-state-cursor) :error (build-error
                                                      "GET"
                                                      (misc/with-baseurl x-url)
                                                      (:status response)
                                                      (:body response))))))))

(defn get-x
  "Make a request via GET."
  ([x-url cb]
   (go
     (let [response (<! (transit-util/request
                         http/get (misc/with-baseurl x-url) nil))
           success (some #{(:status response)} success-status-codes)]
       (if success
         (when cb
           (cb response))
         (om/update! (state/app-state-cursor) :error (build-error
                                                      "GET"
                                                      (misc/with-baseurl x-url)
                                                      (:status response)
                                                      (:body response)))))))
  ([x-url params cb]
   (go
     (let [response (<! (transit-util/request
                         http/get (misc/with-baseurl x-url) params))
           success (some #{(:status response)} success-status-codes)]
       (if success
         (when cb
           (cb response))
         (om/update! (state/app-state-cursor) :error (build-error
                                                      "GET"
                                                      (misc/with-baseurl x-url)
                                                      (:status response)
                                                      (:body response))))))))

(defn post-x
  "POST state"
  ([resource params cb] (post-x resource params cb nil))
  ([resource params cb failcb]
   (go
     (let [response (<! (transit-util/request http/post (misc/with-baseurl resource)
                                              params))
           success (some #{(:status response)} success-status-codes)]
       (if success
         (when cb
           (cb response))
         (do
           (when failcb
             (failcb))
           (om/update! (state/app-state-cursor) :error (build-error
                                                        "POST"
                                                        (misc/with-baseurl resource)
                                                        params
                                                        (:status response)
                                                        (:body response)))))))))

(defn post-x-raw
  "POST state"
  ([resource params cb] (post-x-raw resource params cb nil))
  ([resource params cb failcb]
   (go
     (let [response (<! (http/post (misc/with-baseurl resource)
                                   {:multipart-params params}))
           success (some #{(:status response)} success-status-codes)]
       (if success
         (when cb
           (cb response))
         (do
           (when failcb
             (failcb))
           (om/update! (state/app-state-cursor) :error (build-error
                                                        "POST"
                                                        (misc/with-baseurl resource)
                                                        params
                                                        (:status response)
                                                        (:body response)))))))))

(defn put-x
  "PUT state"
  ([resource params cb] (put-x resource params cb nil))
  ([resource params cb failcb]
   (go
     (let [response (<! (transit-util/request http/put (misc/with-baseurl resource)
                                              params))
           success (some #{(:status response)} success-status-codes)]
       (if success
         (when cb
           (cb response))
         (do
           (when failcb
             (failcb))
           (om/update! (state/app-state-cursor) :error (build-error
                                                        "PUT"
                                                        (misc/with-baseurl resource)
                                                        params
                                                        (:status response)
                                                        (:body response)))))))))

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
    (let [response (<! (transit-util/request http/get (misc/with-baseurl resource)
                                             nil))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "GET"
                                                     (misc/with-baseurl resource)
                                                     (:status response)
                                                     (:body response)))))))

(defn put-resource
  "PUT resource state"
  [resource params cb]
  (go
    (let [response (<! (transit-util/request http/put (misc/with-baseurl resource)
                                             params))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "PUT"
                                                     (misc/with-baseurl resource)
                                                     params
                                                     (:status response)
                                                     (:body response)))))))

(defn post-resource
  "POST resource state"
  [resource params cb]
  (go
    (let [response (<! (transit-util/request http/post (misc/with-baseurl resource)
                                             params))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "POST"
                                                     (misc/with-baseurl resource)
                                                     params
                                                     (:status response)
                                                     (:body response)))))))

(defn delete-resource
  "DELETE resource"
  [resource params cb]
  (go
    (let [response (<! (transit-util/request http/delete (misc/with-baseurl resource)
                                             params))
          success (some #{(:status response)} success-status-codes)]
      (if success
        (when cb
          (cb response))
        (om/update! (state/app-state-cursor) :error (build-error
                                                     "DELETE"
                                                     (misc/with-baseurl resource)
                                                     params
                                                     (:status response)
                                                     (:body response)))))))
