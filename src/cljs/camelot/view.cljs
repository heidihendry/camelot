(ns camelot.view
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.state :as state]
            [camelot.component.nav :as cnav]
            [camelot.component.analysis :as analysis]
            [camelot.component.albums :as calb]
            [camelot.component.settings :as cset]
            [camelot.component.error :as cerr]
            [camelot.component.footer :as cfoot]
            [secretary.core :as secretary :refer-macros [defroute]]))

(defn navbar
  "Render the navbar"
  []
  (om/root cnav/nav-component state/app-state
           {:target (js/document.getElementById "navigation")}))

(def footer
  "Render the footer"
  (om/root cfoot/footer-component state/app-state
           {:target (js/document.getElementById "footer")}))

(defn generate-view
  "Render the main page content"
  [view]
  (om/root view state/app-state
           {:target (js/document.getElementById "page-content")}))

(defn settings-menu-view
  "Render the settings panel"
  []
  (om/root cset/settings-view-component state/app-state
           {:target (js/document.getElementById "settings")}))

(defroute "/#/dashboard" [] (generate-view calb/album-view-component))
(defroute "/#/analysis" [] (generate-view analysis/analysis-view-component))
(defroute "*" [] (generate-view cerr/not-found-page-component))
