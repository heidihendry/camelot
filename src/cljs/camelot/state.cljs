(ns camelot.state
  (:require [om.core :as om :include-macros true]
            [cljs.reader :as reader]))

(defonce app-state
  (atom {}))

(defn app-state-cursor
  []
  (om/ref-cursor (om/root-cursor app-state)))

(defn get-survey-id
  []
  (get-in (app-state-cursor) [:selected-survey :survey-id :value]))

(defn screen-config-state
  [screen]
  (om/ref-cursor (:schema (get (:screens (om/root-cursor app-state)) screen))))

(defn metadata-schema-state
  []
  (om/ref-cursor (:metadata (om/root-cursor app-state))))

(defn display-state
  []
  (om/ref-cursor (:display (om/root-cursor app-state))))

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

(defn library-state
  []
  (om/ref-cursor (:library (om/root-cursor app-state))))

(defn bulk-import-state
  []
  (om/ref-cursor (:bulk-import (om/root-cursor app-state))))

(defn import-dialog-state
  []
  (when (nil? (:import-dialog (om/root-cursor app-state)))
    (om/update! (app-state-cursor) :import-dialog {:visible false}))
  (om/ref-cursor (:import-dialog (om/root-cursor app-state))))
