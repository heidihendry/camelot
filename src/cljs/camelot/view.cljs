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
                 :selected-resource {}})
    (let [f (screens/build-view-component :content)]
      (om/root f state/app-state
               {:target (js/document.getElementById "page-content")}))))

(defroute "/#/dashboard" [] (generate-view calb/album-view-component))
(defroute "/#/surveys" [] (page-content-view :survey :create))
(defroute "/#/survey/:id" [id] (page-content-view :survey :update id))
(defroute "/#/sites" [] (page-content-view :site :create))
(defroute "/#/site/:id" [id] (page-content-view :site :update id))
(defroute "/#/cameras" [] (page-content-view :camera :create))
(defroute "/#/camera/:id" [id] (page-content-view :camera :update id))
(defroute "/#/analysis" [] (generate-view analysis/analysis-view-component))
(defroute "*" [] (generate-view cerr/not-found-page-component))
