(ns camelot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.nav :as snav]
            [secretary.core :as secretary :refer-macros [defroute]]
            [camelot.init :as init]
            [camelot.view :as view]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [cljs.core.async :refer [<!]]
            [weasel.repl :as repl]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog.date UtcDateTime]))

#_(set! *warn-on-infer* true)

(enable-console-print!)

(when-not (repl/alive?)
  (repl/connect "ws://localhost:9001"))

(def reload secretary/dispatch!)

(defn default-page [hash]
  (if (= hash "")
    "/organisation"
    (str "/" hash)))

(defn navigate-dwim
  []
  (-> js/document
      .-location
      .-hash
      default-page
      (snav/nav!)
      (secretary/dispatch!)))

(defn initialise-state
  []
  (rest/get-application
   #(om/update! (state/app-state-cursor) :application (:body %)))
  (view/navbar)
  (init/init-screen-state #(do
                             (view/settings-menu-view)
                             (navigate-dwim))))

(defn initialise-application
  []
  (rest/get-configuration
   #(do (om/update! (state/app-state-cursor) :resources {})
        (om/update! (state/resources-state) :settings (:body %))
        (initialise-state))))

(secretary/set-config! :prefix "#")

(defonce initial-state
  (initialise-application))
