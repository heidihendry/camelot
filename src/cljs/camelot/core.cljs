(ns camelot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.nav :as snav]
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

(defn default-page [hash]
  (if (= hash "")
    "/organisation"
    (str "/" hash)))

(defn disable-loading-screen
  []
  (js/setTimeout
   (fn []
     (set! (.. (js/document.getElementById "loading") -style -cssText) "display: none")
     (set! (.. (js/document.getElementById "navigation") -style -cssText) "")
     (set! (.. (js/document.getElementById "app") -style -cssText) ""))
   1000))

(def reload secretary/dispatch!)

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
   #(do (om/update! (state/app-state-cursor) :application (:body %))
         (view/navbar)))
  (rest/get-screens
   #(do (om/update! (state/app-state-cursor) :screens (:body %))
        (om/update! (state/app-state-cursor) :bulk-import {})
        (om/update! (state/app-state-cursor) :library {:search {}
                                                       :search-results {}})
        (rest/get-resource "/surveys"
                           (fn [r] (om/update! (state/app-state-cursor) :survey {:list (:body r)})))
        (om/update! (state/app-state-cursor) :language :en)
        (om/update! (state/app-state-cursor) :display {:error nil})
        (om/update! (state/app-state-cursor) :view
                    {:settings {:screen {:type :settings
                                         :mode :update}
                                :selected-resource {:details (get (state/resources-state) :settings)}}})
        (om/update! (get-in (state/app-state-cursor) [:view :settings])
                    :buffer (deref (get (state/resources-state)
                                        (get-in (state/app-state-cursor)
                                                [:view :settings :screen :type]))))
        (view/settings-menu-view)
        (disable-loading-screen)
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
