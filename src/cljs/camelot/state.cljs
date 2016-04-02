(ns camelot.state
  (:require [om.core :as om :include-macros true]))

(defonce app-state
  (atom
   {:nav
    {:menu-items [{:url "/dashboard" :label "Dashboard"}
                  {:url "/settings" :label "Settings"}]}}))

(defn app-state-cursor
  []
  (om/ref-cursor (om/root-cursor app-state)))

(defn config-state
  []
  (om/ref-cursor (:config (om/root-cursor app-state))))

(defn nav-state
  []
  (om/ref-cursor (:nav (om/root-cursor app-state))))
