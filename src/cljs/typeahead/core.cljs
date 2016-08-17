(ns typeahead.core
  (:require [om.core :as om]
            [cljs.core.async :refer [<! chan >!]]
            [om.dom :as dom]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn- into-index
  "Add a list of characters to an index, implemented as a trie."
  [idx cs]
  (if (empty? cs)
    (if (empty? idx)
      nil
      (assoc idx "" nil))
    (let [next (into-index (get idx (first cs)) (rest cs))]
      (assoc idx (first cs)
             (if (and (contains? idx (first cs))
                      (nil? (get idx (first cs))))
               (merge {"" nil} next)
               next)))))

(defn index-single
  "Given an existing index, add a new word."
  [idx word]
  (into-index idx (seq word)))

(defn- transforming-index
  [transformer phrases]
  (->> phrases
       (mapcat transformer)
       distinct
       (reduce index-single {})))

(defn word-index
  "Index the words of a list of phrases, returning the result as a trie."
  [phrases]
  (transforming-index #(str/split % #"\W") phrases))

(defn phrase-index
  "Index a list of phrases, returning the result as a trie."
  [phrases]
  (transforming-index list phrases))

(defn completion-list-component
  [data owner {:keys [show-create result-limit]}]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "typeahead-menu"}
               ""))))

(defn typeahead
  "Input component with typeahead-style completion."
  [data owner {:keys [placeholder create-text create-fn result-limit]}]
  (reify
    om/IInitState
    (init-state [_]
      {::value ""
       ::placeholder (or placeholder "")
       ::chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [c (om/get-state owner ::chan)]
        (go
          (loop []
            (let [r (<! c)]
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "typeahead"}
               (dom/input #js {:placeholder (::placeholder state)
                               :value (::value state)
                               :onChange #(>! (::chan state) (.. % -target -value))})
               (om/build completion-list-component completions
                         {:init-state {:chan (select-keys state [::chan])}
                          :opts {:show-create (not (nil? create-fn))
                                 :result-limit result-limit}})))))
