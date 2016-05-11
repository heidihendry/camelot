(ns camelot.nav
  (:require [goog.events :as events]
            [goog.history.EventType :as EventType]
            [secretary.core :as secretary :refer-macros [defroute]]
            [om.core :as om]
            [camelot.state :as state])
  (:import [goog.history Html5History EventType]))

(defn- get-token
  "Get the current location token"
  []
  (str js/window.location.pathname js/window.location.hash))

(defn- make-history
  "Initialise the HTML5 History"
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

(defn analytics-event
  [component action]
  (let [ga (aget js/window "ga")]
    (when ga
      (ga "send" "event" component action))))

(defn analytics-pageview
  [page]
  (let [ga (aget js/window "ga")]
    (when ga
      (ga "set" "page" page)
      (ga "send" "pageview"))))

(defn set-token!
  [history token]
  (analytics-pageview token)
  (.setToken history token))

(defn breadnav!
  "Navigate to a URL token, creating a breadcrumb"
  [token breadcrumb]
  (prn (get-token))
  (om/transact! (state/app-state-cursor) :nav-history
                (fn [h] (conj (vec h) {:token (get-token)
                                       :label breadcrumb})))
  (set-token! history token))

(defn breadnav-consume!
  "Navigate to a URL token"
  [token]
  (om/transact! (state/app-state-cursor) :nav-history
                (fn [h] (take-while #(not= (:token %) token) h)))
  (set-token! history token))

(defn nav!
  "Navigate to a URL token"
  [token]
  (om/update! (state/app-state-cursor) :nav-history [])
  (set-token! history token))

(defn settings-hide!
  "Hide the settings panel"
  []
  (let [elt (js/document.getElementById "settings")
        navelt (js/document.getElementById "settings-nav")]
    (set! (.-className elt) "")
    (set! (.-className navelt) (clojure.string/replace-first
                                (.-className navelt) #"active" ""))))

(defn settings-show!
  "Show the settings panel"
  []
  (let [elt (js/document.getElementById "settings")
        navelt (js/document.getElementById "settings-nav")]
    (set! (.-className elt) "show")
    (set! (.-className navelt) (str "active " (.-className navelt)))))

(defn toggle-settings!
  "Toggle the settings panel show state"
  []
  (let [navelt (js/document.getElementById "settings-nav")]
    (if (clojure.string/includes? (.-className navelt) "active")
      (settings-hide!)
      (settings-show!))))
