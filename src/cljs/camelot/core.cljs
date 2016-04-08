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
               nil
               #(om/update! (state/app-state-cursor) :application (:body %)))
  (util/getreq (util/with-baseurl "/default-config")
               nil
               #(do
                  (util/ls-set-item! "config" (:body %))
                  (om/update! (state/app-state-cursor) :config (:body %))
                  (om/update! (state/app-state-cursor) :config-buffer (:body %))
                  (view/settings-menu-view)
                  (util/getreq (util/with-baseurl "/albums")
                                nil
                                  (fn [x] (if (= (type (:body x)) js/String)
                                            (js/alert (:body x))
                                            (om/update! (state/app-state-cursor) :albums (:body x)))))
                  (util/getreq (util/with-baseurl "/settings")
                               nil
                               (fn [x] (om/update! (state/app-state-cursor) :settings (:body x)))))))

(defn setup
  []
  (util/ls-remove-item! "config")
  (let [config (util/ls-get-item "config")]
    (if config
      (do
        (util/getreq (util/with-baseurl "/application")
                     nil
                     #(om/update! (state/app-state-cursor) :application (:body %)))
        (om/update! (state/app-state-cursor) :config config)
        (om/update! (state/app-state-cursor) :config-buffer config)
        (view/settings-menu-view)
        (when (nil? (:albums config))
          (util/getreq (util/with-baseurl "/albums")
                       nil
                        #(if (= (type (:body %)) js/String)
                           (js/alert (:body %))
                           (om/update! (state/app-state-cursor) :albums (:body %)))))
        (util/getreq (util/with-baseurl "/settings")
                      nil
                      #(om/update! (state/app-state-cursor) :settings (:body %))))
      (config-default state/app-state))))

(secretary/set-config! :prefix "#")

(defn default-page [hash]
  (if (= hash "")
    "/#/dashboard"
    (str "/" hash)))

(defn disable-loading
  []
  (set! (.-style (js/document.getElementById "loading")) "display: none")
  (set! (.-style (js/document.getElementById "navigation")) "")
  (set! (.-style (js/document.getElementById "app")) "")
  (set! (.-style (js/document.getElementById "footer")) ""))

(or ;(:config @app-state)
 (do (setup)
     (disable-loading)
     (-> js/document
         .-location
         .-hash
         default-page
         (nav/nav!))))
