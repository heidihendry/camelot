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
      (when (seq data)
        (dom/div #js {:className "typeahead-menu"}
                 (map (fn [x] (dom/div #js {
                                            :className "menu-item"
                                            :onClick #(go (>! (::chan state)
                                                              {:select x}))} x))
                      data))))))

(defn match-builder
  [data]
  (if (nil? data)
    [""]
    (->> data
         keys
         (mapcat #(map (fn [x] (str % x)) (match-builder (get data %)))))))

(defn search-reducer
  [acc s]
  (let [r (get acc s)]
    (if (nil? r)
      (reduced {})
      r)))

(defn matches
  [data search]
  (->> (seq search)
       (reduce search-reducer data)
       match-builder
       (map #(str search %))
       (sort)
       (sort-by count)))

(defn typeahead
  "Input component with typeahead-style completion."
  [data owner {:keys [create-text create-fn input-config]}]
  (reify
    om/IInitState
    (init-state [_]
      {::value ""
       ::chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [c (om/get-state owner ::chan)]
        (go
          (loop []
            (let [r (<! c)]
              (if (:select r)
                (do
                  (om/set-state! owner ::value (:select r))
                  (.focus (om/get-node owner "searchInput")))
                (om/set-state! owner ::value r))
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (let [v (::value state)]
        (dom/div #js {:className "typeahead"}
                 (dom/input (clj->js (merge input-config
                                            {:value v
                                             :ref "searchInput"
                                             :onChange #(do
                                                          (when (:onChange input-config)
                                                            ((:onChange input-config) %))
                                                          (let [tv (.. % -target -value)]
                                                            (when tv
                                                              (go (>! (::chan state) tv)))))})))
                 (when-not (empty? v)
                   (om/build completion-list-component (matches data v)
                             {:init-state {::chan (::chan state)}
                              :opts {:show-create (not (nil? create-fn))}})))))))
