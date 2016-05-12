(ns camelot.util.screen
  (:require [camelot.state :as state]))

(defn get-screen
  "Return the screen corresponding to the given view-state."
  [vs]
  (get (get (state/app-state-cursor) :screens) (get-in vs [:screen :type])))

(defn get-screen-resource
  "Return the resource `k' of the screen of the given view-state."
  [vs k]
  (get-in (get-screen vs) [:resource k]))

(defn get-endpoint
  "Return the current resource's endpoint."
  [vs]
  (get-screen-resource vs :endpoint))

(defn get-screen-title
  "Return the title of the screen of the given view-state."
  [vs]
  (get-screen-resource vs :title))

(defn get-resource-name
  "Get the name for the current screen's resource type."
  [vs]
  (name (get-in (get-screen vs) [:resource :type])))

(defn get-resource-id
  "Return the ID of the current resource for this view-state."
  [vs]
  (let [rid (get-screen-resource vs :id)]
    (if (nil? rid)
      nil
      (get-in vs [:selected-resource :details rid :value]))))

(defn get-parent-resource-id
  "Return the parent resource's endpoint URL for the current view-state."
  [vs]
  (get-in vs [:screen :id]))

(defn get-url
  "Return the endpoint URL for the current view-state."
  [vs]
  (let [rid (get-resource-id vs)
        base (get-endpoint vs)]
    (if (nil? rid)
      base
      (str base "/" rid))))

(defn settings-screen?
  "Predicate for whether this view-state is for the settings screen."
  [vs]
  (= (get-screen-resource vs :type) :settings))
