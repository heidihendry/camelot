(ns camelot.state
  (:require [om.core :as om :include-macros true]
            [cljs.reader :as reader]))

(defn remove-item!
  [val data edit-key owner]
  (om/transact! data edit-key
                (fn [_] (->>
                         edit-key
                         (get data)
                         (deref)
                         (remove #{val})
                         (into [])))))

(defn add-item!
  [val data edit-key owner]
  (when (not (empty? val))
    (om/transact! data edit-key
                  (fn [_]
                    (->> [edit-key :value]
                         (get-in data)
                         (#(conj % val))
                         (into #{})
                         (into [])
                         (hash-map :value))))))

(defn add-metadata-item!
  [val data edit-key owner]
  (when (not (empty? val))
    (om/transact! data edit-key (fn [_]
                                  (->> [edit-key :value]
                                       (get-in data)
                                       (deref)
                                       (#(conj % val))
                                       (into [])
                                       (hash-map :value))))))

(defn set-unvalidated-text! [e data edit-key owner]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn set-number! [e data edit-key owner]
  (if (re-matches #"^-?[\.0-9]*$" (.. e -target -value))
    (om/transact! data edit-key (fn [_] (reader/read-string (.. e -target -value))))
    (set! (.. e -target -value) (get data edit-key))))

(defn set-percentage! [e data edit-key owner]
  (let [input (.. e -target -value)]
    (if (and (re-matches #"^[.0-9]*$" input)
             (<= (reader/read-string input) 1.0))
      (om/transact! data edit-key (fn [_] (reader/read-string input)))
      (set! (.. e -target -value) (get data edit-key)))))

(defn set-coerced-value!
  [k]
  (fn [e data edit-key owner]
    (let [input (.. e -target -value)
          f (cond (= (type k) cljs.core/Keyword) (fn [_] (keyword input))
                  (number? k) (fn [_] (reader/read-string input))
                  :else (fn [_] input))]
      (om/transact! data edit-key f))))

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
