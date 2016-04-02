(ns camelot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.nav :as nav]
            [camelot.view :as view]
            [camelot.state :as state]
            [camelot.util :as util]
            [cljs.core.async :refer [<!]]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog.date UtcDateTime]))

(enable-console-print!)

(defn config-default
  [state]
  (util/getreq (util/with-baseurl "/application")
               {}
               #(om/update! (state/app-state-cursor) :application (:body %)))
  (util/getreq (util/with-baseurl "/default-config")
               {}
               #(do
                  (util/ls-set-item! "config" (:body %))
                  (om/update! (state/app-state-cursor) :config (:body %))
                    (util/postreq (util/with-baseurl "/albums")
                                  {:config (:body %) :dir "/home/chris/testdata"}
                                  (fn [x] (om/update! (state/app-state-cursor) :albums (:body x))))
                    (util/postreq (util/with-baseurl "/settings/get")
                                  {:config (:config state)}
                                  (fn [x] (om/update! (state/app-state-cursor) :settings (:body x)))))))

(defn setup
  []
  (util/ls-remove-item! "config")
  (let [config (util/ls-get-item "config")]
    (if config
      (do
        (util/getreq (util/with-baseurl "/application")
                     {}
                     #(om/update! (state/app-state-cursor) :application (:body %)))
        (om/update! (state/app-state-cursor) :config config)
        (when (nil? (:albums config))
          (util/postreq (util/with-baseurl "/albums")
                        {:config config :dir "/home/chris/testdata"}
                        #(om/update! (state/app-state-cursor) :albums (:body %))))
        (util/postreq (util/with-baseurl "/settings/get")
                      {:config config}
                      #(om/update! (state/app-state-cursor) :settings (:body %))))
      (config-default state/app-state))))

(defn default-page [page]
  (if (= page "/")
    "/dashboard"
    page))

(or ;(:config @app-state)
    (do (setup)
        (-> js/document
            .-location
            .-pathname
            default-page
            (nav/nav!))))
