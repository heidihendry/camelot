(ns camelot.nav
  (:require [goog.events :as events]
            [goog.history.EventType :as EventType]
            [secretary.core :as secretary :refer-macros [defroute]]
            [om.core :as om]
            [camelot.state :as state]
            [clojure.string :as str])
  (:import [goog History]
           [goog.history EventType]))

(defn- get-token
  "Get the current location token"
  []
  (str js/window.location.pathname js/window.location.hash))

(defonce history
  (doto (History.)
    (goog.events/listen EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
    (.setEnabled true)))

(defn survey-url
  [& paths]
  (str "/"
       (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])
       "/"
       (str/join "/" paths)))

(defn analytics-event
  ([component action]
   (when js/window.ga
     (js/window.ga "send" "event" component action)))
  ([component action label]
   (when js/window.ga
     (js/window.ga "send" "event" component action label))))

(defn analytics-pageview
  [page]
  (when js/window.ga
    (js/window.ga "set" "page" page)
    (js/window.ga "send" "pageview")))

(defn set-token!
  [history token]
  (let [token (if (= (subs token 0 2) "/#")
                (subs token 2)
                token)]
    (analytics-pageview token)
    (.setToken history token)
    token))

(defn breadnav!
  "Navigate to a URL token, creating a breadcrumb"
  [token breadcrumb state]
  (om/transact! (state/app-state-cursor) :nav-history
                (fn [h] (conj (vec h) {:token (get-token)
                                       :label breadcrumb
                                       :state (deref state)})))
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

