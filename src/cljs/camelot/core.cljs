(ns camelot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.util :as util]
            [camelot.route.albums :as ra]
            [camelot.route.settings :as rs]
            [camelot.route.error :as re]
            [cljs.core.async :refer [<!]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import [goog.date UtcDateTime]
           [goog.history Html5History EventType]))

(defonce app-state (atom
                    {:nav
                     {:menu-items [{:url "/dashboard" :label "Dashboard"}
                                   {:url "/settings" :label "Settings"}]}}))

(enable-console-print!)

(defn- get-token []
  (str js/window.location.pathname js/window.location.search))

(defn- make-history
  []
  (doto (Html5History.)
    (.setPathPrefix (str js/window.location.protocol
                         "//"
                         js/window.location.host))
    (.setUseFragment false)))

(defonce history
  (doto (make-history)
    (goog.events/listen EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (.setEnabled true)))

(defn nav! [token]
  (do
    (.setToken history token)))

(defn album-component-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil (apply dom/div nil (om/build-all ra/albums-component (:albums app)))))))

(defn settings-component-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h2 nil "Settings")
               (dom/div nil (om/build rs/settings-component (:config app)))))))

(defn nav-item-component [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/li #js {:onClick #(nav! (:url data))} (dom/a nil (:label data))))))

(defn nav-component [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/ul #js {:className "nav navbar-nav"}
             (om/build-all nav-item-component (:menu-items data))))))

(defn footer-component [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "version"} "Camelot v0.1.0")
               (dom/label #js {:className "developer"}
                          (dom/a {:href "chris@bitpattern.com.au"} "Bit Pattern")
                          " © 2016 ")))))

(defn generate-view [view]
  (om/root view app-state {:target (js/document.getElementById "app")}))

(defn baseurl [path]
  (let [port (-> js/window (aget "location") (aget "port"))]
    (str
     (-> js/window (aget "location") (aget "protocol"))
     "//"
     (-> js/window (aget "location") (aget "hostname"))
     (when (not (zero? (count port)))
       (str ":" port))
     path)))

(defn config-default
  [state]
  (util/getreq (baseurl "/default-config")
               {}
               #(do (util/ls-set-item! "config" (:body %))
                    (om/update! (om/ref-cursor (om/root-cursor app-state)) :config (:body %))
                    (util/postreq (baseurl "/albums")
                                  {:config (:body %) :dir "/home/chris/testdata"}
                                  (fn [x] (om/update! (om/ref-cursor (om/root-cursor app-state)) :albums (:body x)))))))

(defroute "/dashboard" [] (generate-view album-component-view))
(defroute "/settings" [] (generate-view settings-component-view))
(defroute "*" [] (generate-view re/not-found-page-component))

(def navbar
  (om/root nav-component (om/ref-cursor (:nav (om/root-cursor app-state))) {:target (js/document.getElementById "navigation")}))

(def footer
  (om/root footer-component app-state {:target (js/document.getElementById "footer")}))

(defn setup
  []
  (util/ls-remove-item! "config")
  (let [config (util/ls-get-item "config")]
    (if config
      (do
        (om/update! (om/ref-cursor (om/root-cursor app-state)) :config config)
        (when (nil? (:albums config))
          (util/postreq (baseurl "/albums")
                        {:config config :dir "/home/chris/testdata"}
                        #(om/update! (om/ref-cursor (om/root-cursor app-state)) :albums (:body %)))))
      (config-default app-state))))

(defn default-page [page]
  (if (= page "/")
    "/dashboard"
    page))

(or (:config @app-state)
    (do (setup)
        (-> js/document
            .-location
            .-pathname
            default-page
            (nav!))))
