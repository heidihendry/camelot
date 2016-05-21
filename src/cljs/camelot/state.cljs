(ns camelot.state
  (:require [om.core :as om :include-macros true]
            [cljs.reader :as reader]))

(defonce app-state
  (atom {}))

(defn app-state-cursor
  []
  (om/ref-cursor (om/root-cursor app-state)))

(defn screen-config-state
  [screen]
  (om/ref-cursor (:schema (get (:screens (om/root-cursor app-state)) screen))))

(defn metadata-schema-state
  []
  (om/ref-cursor (:metadata (om/root-cursor app-state))))

(defn view-state
  [area]
  {:pre [(or (= area :settings) (= area :content))]}
  (om/ref-cursor (get (:view (om/root-cursor app-state)) area)))

(defn config-buffer-state
  []
  (om/ref-cursor (:config-buffer (om/root-cursor app-state))))

(defn resources-state
  []
  (om/ref-cursor (:resources (om/root-cursor app-state))))

(defn config-state
  []
  (om/ref-cursor (:config (resources-state))))

(defn nav-state
  []
  (om/ref-cursor (:nav (:application (om/root-cursor app-state)))))
