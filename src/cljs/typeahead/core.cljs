(ns typeahead.core
  "Input field with flexible typeahead."
  (:require [om.core :as om]
            [cljs.core.async :refer [<! chan >!]]
            [om.dom :as dom]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def term-separator-re #"[|+ :]")

(defn- into-index
  "Add a list of characters to an index, implemented as a trie."
  [idx cs]
  (if (empty? cs)
    (if (empty? idx)
      nil
      (assoc idx "" nil))
    (let [c (first cs)
          next (into-index (get idx c) (rest cs))]
      (assoc idx c (if (and (contains? idx c)
                            (nil? (get idx c)))
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
  (transforming-index #(str/split % term-separator-re) phrases))

(defn phrase-index
  "Index a list of phrases, returning the result as a trie."
  [phrases]
  (transforming-index list phrases))

(defn completion-option-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {
                    :className "menu-item"
                    :onClick #(go (>! (::chan state)
                                      {::select (:completion data)}))}
               (:completion data)))))

(defn completion-list-component
  [data owner {:keys [show-create result-limit]}]
  (reify
    om/IRenderState
    (render-state [_ state]
      (when (seq data)
        (dom/div #js {:className "typeahead-menu"}
                 (om/build-all completion-option-component
                               data
                               {:init-state state
                                :key :completion}))))))

(defn- match-builder
  [data]
  (if (nil? data)
    [""]
    (->> data
         keys
         (mapcat #(map (fn [x] (str % x)) (match-builder (get data %)))))))

(defn- search-reducer
  [acc s]
  (let [r (get acc s)]
    (if (nil? r)
      (reduced {})
      r)))

(defn complete
  "Return all possible completions given the index data and a prefix."
  [data prefix]
  (->> (seq prefix)
       (reduce search-reducer data)
       match-builder
       (map #(str prefix %))
       (sort)
       (sort-by count)))

(defn- term-separators
  [search]
  (->> search
       seq
       (map-indexed #(vector %1 %2))
       (filter #(re-matches term-separator-re (second %)))
       (map first)))

(defn- next-separator
  [search point]
  (or (first (filter #(>= % point) (term-separators search)))
      (count search)))

(defn term-at-point
  "Return the term at a given cursor position."
  [search point]
  (let [p (next-separator search (min (count search) point))]
    (if (or (re-matches term-separator-re (str (nth search (dec point))))
            (zero? point))
      ""
      (last (str/split (apply str (first (split-at p search)))
                       term-separator-re)))))

(defn splice
  [all new start end]
  (apply conj
         (apply conj
                (vec (take start all))
                new)
         (let [tail (drop end all)]
           (if (= (first tail) ":")
             (drop 1 tail)
             tail))))

(defn replace-term
  [search point insertion multi-term]
  (if multi-term
    (let [term (term-at-point search point)
          end (next-separator search point)]
      (apply str (splice (seq search) (seq insertion) (- end (count term)) end)))
    insertion))

(defn typeahead
  "Input component with typeahead-style completion."
  [data owner {:keys [create-text create-fn input-config multi-term]}]
  (reify
    om/IInitState
    (init-state [_]
      {::value ""
       ::chan (chan)})
    om/IWillUpdate
    (will-update [_ props state]
      (let [si (om/get-node owner "search-input")]
        (om/set-state! owner
                       ::term
                       ((if multi-term
                          term-at-point
                          identity) (::value state)
                          (.-selectionStart si)))))
    om/IWillMount
    (will-mount [_]
      (let [c (om/get-state owner ::chan)]
        (go
          (loop []
            (let [r (<! c)]
              (if (::select r)
                (do
                  (let [si (om/get-node owner "search-input")]
                    (om/set-state! owner ::value (replace-term (om/get-state owner ::value)
                                                               (.-selectionStart si)
                                                               (::select r)
                                                               multi-term)))
                  (.focus (om/get-node owner "search-input"))))
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (let [v (::term state)]
        (dom/div #js {:className "typeahead"}
                 (dom/input (clj->js (merge input-config
                                            {:value (::value state)
                                             :ref "search-input"
                                             :onChange #(do
                                                          (when (:onChange input-config)
                                                            ((:onChange input-config) %))
                                                          (let [tv (.. % -target -value)]
                                                            (om/set-state! owner ::value tv)
                                                            (when tv
                                                              (go (>! (::chan state) tv)))))})))
                 (when-not (empty? v)
                   (om/build completion-list-component
                             (map #(hash-map :completion %
                                             :context nil) (complete data v))
                             {:init-state {::chan (::chan state)}
                              :opts {:show-create (not (nil? create-fn))}})))))))
