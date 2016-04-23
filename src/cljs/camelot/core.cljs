(ns camelot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.nav :as nav]
            [camelot.view :as view]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [camelot.util :as util]
            [camelot.component.albums :as albums]
            [cljs.core.async :refer [<!]]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog.date UtcDateTime]))

(enable-console-print!)

(defn initialise-state
  []
  (rest/get-application
   #(do (om/update! (state/app-state-cursor) :application (:body %))
         (view/navbar)))
  (rest/get-screens
   #(do (om/update! (state/app-state-cursor) :screens (:body %))
        (om/update! (state/app-state-cursor) :view
                    {:settings {:screen {:type :settings
                                         :mode :update}}})
        (view/settings-menu-view))))

(defn initialise-application
  []
  (rest/get-metadata
   (fn [x]
     (om/update! (state/app-state-cursor) :metadata (:body x))
     (rest/get-configuration
      #(do (om/update! (state/app-state-cursor) :resources {})
           (om/update! (state/resources-state) :config (:body %))
           (initialise-state)
           (albums/reload-albums))))))

(secretary/set-config! :prefix "#")

(defn default-page [hash]
  (if (= hash "")
    "/#/dashboard"
    (str "/" hash)))

(defn disable-loading-screen
  []
  (set! (.-style (js/document.getElementById "loading")) "display: none")
  (set! (.-style (js/document.getElementById "navigation")) "")
  (set! (.-style (js/document.getElementById "app")) "")
  (set! (.-style (js/document.getElementById "footer")) ""))

(defn navigate-dwim
  []
  (-> js/document
      .-location
      .-hash
      default-page
      (nav/nav!)))

(defonce initial-state
  (do (initialise-application)
      (disable-loading-screen)
      (navigate-dwim)))
