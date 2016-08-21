(ns typeahead.core
  "Input field with powerful typeahead."
  (:require [om.core :as om]
            [cljs.core.async :refer [<! chan >! alts!]]
            [om.dom :as dom]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def term-separator-re #"[|+ :]")

(defn ->basic-entry
  [term]
  {:term term
   :props {}})

(defn- into-index
  "Add a list of characters to an index, implemented as a trie."
  [idx cs props]
  (if (empty? cs)
    (if (empty? idx)
      props
      (assoc idx "" props))
    (let [c (first cs)
          next (into-index (get idx c) (rest cs) props)]
      (assoc (dissoc idx :props) c
             (if (contains? (get idx c) :props)
               (merge {"" (get idx c)} next)
               next)))))

(defn index-single
  "Given an existing index, add a new word."
  [idx {:keys [term props]}]
  (into-index idx (seq term) {:props props}))

(defn- transforming-index
  [transformer phrases]
  (->> phrases
       (mapcat transformer)
       distinct
       (reduce index-single {})))

(defn word-index
  "Index the words of a list of phrases, returning the result as a trie."
  [phrases]
  (transforming-index #(map (fn [x] {:term x :props (or (:props %) {})})
                            (str/split (:term %) term-separator-re))
                      phrases))

(defn phrase-index
  "Index a list of phrases, returning the result as a trie."
  [phrases]
  (transforming-index #(list (assoc % :props (or (:props %) {}))) phrases))

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
  (if (contains? data :props)
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
       (sort #(compare (str/lower-case %1) (str/lower-case %2)))))

(defn- props-for-subtree
  [st]
  (or (:props st)
      (get-in st ["" :props])))

(defn ifind
  "Return the props for search, or nil if search is not in the index."
  [index search]
  (if (nil? search)
    nil
    (->> (seq search)
         (reduce search-reducer index)
         props-for-subtree)))

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

(defn field-context
  "Return the field responsible for setting context at point, if any."
  [search point]
  (if (zero? point)
    nil
    (reduce #(let [c (nth search %2)]
               (cond
                 (or (= c "|") (= c " "))
                 (reduced nil)

                 (= c ":")
                 (reduced (term-at-point search %2))

                 :else nil))
            nil
            (range (dec point) 0 -1))))

(defn typeahead
  "Input component with typeahead-style completion."
  [data owner {:keys [create-text create-fn input-config multi-term]}]
  (reify
    om/IInitState
    (init-state [_]
      {::value ""
       ::int-chan (chan)
       ::completion-chan (chan)})
    om/IWillUpdate
    (will-update [_ props state]
      (let [si (om/get-node owner "search-input")
            ctx (field-context (::value state)
                               (.-selectionStart si))]
        (when-not (= (::context state) ctx)
          (om/set-state! owner ::context ctx)
          (om/set-state! owner ::completions nil)
          (let [cfn (:completion-fn (ifind data ctx))]
            (and cfn (cfn ctx (::completion-chan state)))))
        (om/set-state! owner
                       ::term
                       ((if multi-term
                          term-at-point
                          identity) (::value state)
                          (.-selectionStart si)))))
    om/IWillMount
    (will-mount [_]
      (let [ic (om/get-state owner ::int-chan)
            cc (om/get-state owner ::completion-chan)]
        (go
          (loop []
            (let [[r port] (alts! [ic cc])]
              (if (= port ic)
                (if (::select r)
                  (do
                    (let [si (om/get-node owner "search-input")
                          props (ifind data (::select r))]
                      (om/set-state! owner ::value
                                     (replace-term (om/get-state owner ::value)
                                                   (.-selectionStart si)
                                                   (str (::select r)
                                                        (if (:field props)
                                                          ":"
                                                          " "))
                                                   multi-term)))
                    (.focus (om/get-node owner "search-input"))))
                (do (prn "Setting completions!")
                    (om/set-state! owner ::completions r)))
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (let [v (::term state)
            ctx (::completions state)]
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
                                                              (go (>! (::int-chan state) tv)))))})))
                 (when-not (and (empty? v) (empty? ctx))
                   (om/build completion-list-component
                             (map #(hash-map :completion %
                                             :context nil) (complete (or ctx data) v))
                             {:init-state {::chan (::int-chan state)}
                              :opts {:show-create (not (nil? create-fn))}})))))))
