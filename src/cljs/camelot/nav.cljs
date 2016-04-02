(ns camelot.nav
  (:require [goog.events :as events]
            [goog.history.EventType :as EventType]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog.history Html5History EventType]))

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
  (.setToken history token))

(defn toggle-settings!
  []
  (let [elt (js/document.getElementById "settings")
        navelt (js/document.getElementById "settings-nav")]
    (if (clojure.string/includes? (.-className navelt) "active")
      (do
        (set! (.-className elt) "")
        (set! (.-className navelt) (clojure.string/replace-first (.-className navelt) #"active" "")))
      (do
        (set! (.-className elt) "show")
        (set! (.-className navelt) (str "active " (.-className navelt)))))))
