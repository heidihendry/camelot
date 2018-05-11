(ns manager.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [goog.object :as obj]
   [cljs.core.async :refer [<!]]
   [cljs-http.client :as http]
   [cljs.nodejs :as nodejs]))

(def host "http://localhost:5341")
(def poll-interval-ms 250)

(enable-console-print!)

(defn navigate-on-startup
  []
  (go
    (let [response (<! (http/get (str host "/heartbeat")))
          success? (= (:status response) 200)]
      (when success?
        (obj/set (-> js/window .-location) "href"
                 (str host "/index.html"))))))

(defn poll-startup
  []
  (js/setInterval navigate-on-startup poll-interval-ms))

(poll-startup)
