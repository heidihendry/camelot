(ns camelot.view
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.state :as state]
            [camelot.component.nav :as cnav]
            [camelot.component.analysis :as analysis]
            [camelot.component.albums :as calb]
            [camelot.component.surveys :as surveys]
            [camelot.component.screens :as screens]
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

(def error-dialog
  "Render the error dialog"
  (om/root cerr/error-dialog-component state/app-state
           {:target (js/document.getElementById "error-dialog")}))

(defn generate-view
  "Render the main page content"
  [view]
  (om/root view state/app-state
           {:target (js/document.getElementById "page-content")}))

(defn settings-menu-view
  "Render the settings panel"
  []
  (let [f (screens/build-view-component :settings)]
    (om/root f state/app-state
             {:target (js/document.getElementById "settings")})))

(defn page-content-view
  [type mode & [id]]
  (when (and (not (nil? (:view (state/app-state-cursor))))
             (not (nil? (:resources (state/app-state-cursor)))))
      (om/update! (get (state/app-state-cursor) :view) :content
                  {:screen {:type type :mode mode :id id} :buffer {}
                   :selected-resource {}
                   :generator-data {}})
    (let [f (screens/build-view-component :content)]
      (om/root f state/app-state
               {:target (js/document.getElementById "page-content")}))))

(defroute "/#/dashboard" [] (generate-view calb/album-view-component))
(defroute "/#/surveys" [] (page-content-view :survey :create))
(defroute "/#/trap-station-session-cameras/:id" [id] (page-content-view :trap-station-session-camera :create id))
(defroute "/#/trap-station-sessions/:id" [id] (page-content-view :trap-station-session :create id))
(defroute "/#/trap-stations/:id" [id] (page-content-view :trap-station :create id))
(defroute "/#/survey-sites/:id" [id] (page-content-view :survey-site :create id))
(defroute "/#/sites" [] (page-content-view :site :create))
(defroute "/#/cameras" [] (page-content-view :camera :create))
(defroute "/#/analysis" [] (generate-view analysis/analysis-view-component))
(defroute "*" [] (generate-view cerr/not-found-page-component))
