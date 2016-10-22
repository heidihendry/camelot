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
  ([]
   (let [survey-id (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])]
     (if (nil? survey-id)
       (str "/organisation")
       (str "/"
            (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])))))
  ([& paths]
   (let [survey-id (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])]
     (if (some nil? (conj paths survey-id))
       (str "/organisation")
       (str "/"
            (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])
            "/"
            (str/join "/" paths))))))

(defn analytics-event
  ([component action]
   (when (get-in (state/resources-state) [:settings :send-usage-data :value])
     (if-let [ga (aget js/window "cljs_ga")]
       (ga "send" "event" component action)
       (.warn js/console "Analytics library not found"))))
  ([component action label]
   (when (get-in (state/resources-state) [:settings :send-usage-data :value])
     (if-let [ga (aget js/window "cljs_ga")]
       (ga "send" "event" component action label)
       (.warn js/console "Analytics library not found")))))

(defn analytics-pageview
  [page]
  (when (get-in (state/resources-state) [:settings :send-usage-data :value])
    (if-let [ga (aget js/window "cljs_ga")]
      (do
        (ga "set" "page" page)
        (ga "send" "pageview")))))

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

(defn nav-up-url
  [token levels]
  {:pre [(and (string? token) (number? levels))]}
  (let [url (reduce #(str/replace % #"(.*)/.+?$" "$1") token (range levels))]
    (if (or (= url "/#") (= url ""))
      "/#/organisation"
      url)))

(defn nav-up!
  "Navigate up 1 or more levels."
  ([] (nav-up! 1))
  ([levels] (nav! (nav-up-url (get-token) levels))))
