(ns camelot.view
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.state :as state]
            [camelot.component.nav :as cnav]
            [camelot.component.albums :as calb]
            [camelot.component.settings :as cset]
            [camelot.component.error :as cerr]
            [camelot.component.footer :as cfoot]
            [secretary.core :as secretary :refer-macros [defroute]]))

(def navbar
  (om/root cnav/nav-component (state/nav-state)
           {:target (js/document.getElementById "navigation")}))

(def footer
  (om/root cfoot/footer-component state/app-state
           {:target (js/document.getElementById "footer")}))

(defn generate-view [view]
  (om/root view state/app-state
           {:target (js/document.getElementById "page-content")}))

(om/root cset/settings-view-component state/app-state {:target (js/document.getElementById "settings")})

(defroute "/dashboard" [] (generate-view calb/album-view-component))
(defroute "/settings" [] (generate-view cset/settings-view-component))
(defroute "*" [] (generate-view cerr/not-found-page-component))
