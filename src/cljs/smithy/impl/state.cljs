(ns smithy.impl.state
  (:require [om.core :as om :include-macros true]
            [clojure.string :as string]
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

(defn set-unvalidated-text! [e data edit-key owner]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn set-flag! [e data edit-key owner]
  (om/transact! data edit-key not))

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
