(ns typeahead.core
  "Input field with powerful typeahead."
  (:require [om.core :as om]
            [cljs.core.async :refer [<! chan >! sliding-buffer alts! timeout]]
            [om.dom :as dom]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def operators
  #{"<" "<=" ">" ">=" ":" "==" "!=" " " "|"})

(def term-separator-re #"\||\+|\ |\:|<=|<|>=|>|==|!=")
(def field-value-separator-re #"(:?:|<=|<|>=|>|==|!=)")

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
  (into-index idx (seq (str/lower-case (str term))) {:props props}))

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
      (dom/div #js {:className (str "menu-item" (if (= (:index data)
                                                       (::selection-index state))
                                                  " active"
                                                  ""))
                    :onClick #(do
                                (om/set-state! owner ::async-pending true)
                                (go (>! (::chan state)
                                        {::select (get-in data [:entry :completion])})))}
               (get-in data [:entry :completion])))))

(defn completion-list-component
  [data owner {:keys [show-create result-limit]}]
  (reify
    om/IRenderState
    (render-state [_ state]
      (when (seq data)
        (dom/div #js {:className "typeahead-menu"}
                 (om/build-all completion-option-component
                               (vec (map-indexed #(hash-map :entry %2
                                                            :index %1)
                                                 data))
                               {:init-state {::chan (::chan state)}
                                :state {::selection-index (mod (::selection-index state)
                                                               (count data))}
                                :key :index}))))))

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
  (->> prefix
       str/lower-case
       seq
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
    (->> search
         str/lower-case
         seq
         (reduce search-reducer index)
         props-for-subtree)))

(defn- separator-positions
  [point s sep]
  (loop [pos (- point (count sep))
         acc []]
    (if-let [p (str/index-of s sep pos)]
      (recur (inc p) (conj acc p))
      acc)))

(defn all-separator-positions
  [point s]
  (mapcat (partial separator-positions point s) operators))

(defn- term-separators
  [search point]
  (->> search
       str/lower-case
       (all-separator-positions point)
       sort
       distinct))

(defn- next-separator
  [search point]
  (or (first (term-separators search point))
      (count search)))

(defn remove-negation
  [term]
  (if (= (first term) \!)
    (subs term 1)
    term))

(defn term-at-point
  "Return the term at a given cursor position."
  [search point]
  (let [p (next-separator search (min (count search) point))]
    (cond
      (or (zero? point)
          (contains? operators (subs search (dec point) point)))
      ""

      :default
      (->> search
           (split-at p)
           first
           (apply str)
           (#(str/split % term-separator-re))
           last
           remove-negation))))

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

(defn insertion-chars
  [insertion insert-after]
  (let [qd (if (re-matches #".*[\ \|].*" insertion)
             (str "\"" insertion "\"")
             insertion)]
    (seq (str qd insert-after))))

(defn term-end
  [search term point]
  (if (= point (count search))
    point
    (or (last (take-while #(= (term-at-point search %) term)
                          (range (inc point) (count search))))
        point)))

(defn replace-term
  [search point insertion insert-after multi-term]
  (if multi-term
    (let [term (term-at-point search point)
          term-end (term-end search term point)]
      (let [m (clj->js (seq search))]
        (.splice m
                 (- term-end (count term))
                 (count term)
                 (str/join "" (insertion-chars insertion insert-after)))
        (.join m "")))
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

                 (contains? #{":" ">" "<" "="} c)
                 (reduced (term-at-point search (dec %2)))

                 :else nil))
            nil
            (range (dec point) 0 -1))))

(defn already-complete?
  [cs v]
  (and (= (count cs) 1)
       (= (first cs) v)))

(defn typeahead
  "Input component with typeahead-style completion."
  [data owner {:keys [create-text create-fn input-config multi-term operator-overrides]}]
  (reify
    om/IInitState
    (init-state [_]
      {::value ""
       ::int-chan (chan (sliding-buffer 1))
       ::completion-chan (chan)
       ::selection-index 0})
    om/IDidUpdate
    (did-update [_ props state]
      (let [value (om/get-state owner ::value)
            si (om/get-node owner "search-input")
            selection-start (.-selectionStart si)
            pos (if (> selection-start (count value))
                  (count value)
                  selection-start)]
        (when (not= (::cursor-position state) pos)
          (om/set-state! owner ::cursor-position pos))
        (let [ctx (field-context value pos)]
          (when-not (= (::context state) ctx)
            (om/set-state! owner ::context ctx)
            (om/set-state! owner ::completions nil)
            (let [cfn (:completion-fn (ifind data ctx))]
              (and cfn (cfn (str/lower-case ctx) (::completion-chan state)))))
          (let [term ((if multi-term
                        term-at-point
                        identity) value pos)]
            (when-not (= (::term state) term)
              (om/set-state! owner ::term term)))))
      (go
        (<! (timeout 300))
        (when (and (om/get-state owner ::clear-focus)
                   (om/get-state owner ::is-focused))
          (om/set-state! owner ::is-focused false))))
    om/IDidMount
    (did-mount [_]
      (let [ic (om/get-state owner ::int-chan)
            cc (om/get-state owner ::completion-chan)]
        (go
          (loop []
            (let [[r port] (alts! [ic cc])]
              (if (= port ic)
                (if (::select r)
                  (do
                    (let [props (ifind data (::select r))
                          suffix (if (:field props)
                                   (get operator-overrides (keyword (::select r)) ":")
                                   "")
                          v (replace-term (om/get-state owner ::value)
                                          (om/get-state owner ::cursor-position)
                                          (str (::select r) suffix)
                                          (if (empty? suffix) " " "")
                                          multi-term)]
                      (om/set-state! owner ::value v)
                      (om/set-state! owner ::async-pending false)
                      (when (:onChange input-config)
                        ((:onChange input-config) v)))
                    (do
                      (.focus (om/get-node owner "search-input")))))
                (om/set-state! owner ::completions r))
              (om/set-state! owner ::selection-index 0)
              (recur)))))
      (let [si (om/get-node owner "search-input")]
        (.addEventListener si "blur" (fn [evt]
                                       (let [t (.-target evt)]
                                         (om/set-state! owner ::clear-focus true))))
        (.addEventListener si "focus" (fn [evt]
                                        (let [t (.-target evt)]
                                          (om/set-state! owner ::is-focused true)
                                          (om/set-state! owner ::clear-focus false))))))
    om/IRenderState
    (render-state [_ state]
      (let [v (::term state)
            ctx (::completions state)]
        (dom/div #js {:className "typeahead"}
                 (dom/input (clj->js (merge input-config
                                            {:value (get state ::value "")
                                             :ref "search-input"
                                             :disabled (if (:disabled state) "disabled" "")
                                             :onKeyDown (fn [e]
                                                          (.stopPropagation e)
                                                          (cond
                                                            (= (.-keyCode e) 38)
                                                            (do (om/update-state! owner ::selection-index dec)
                                                                (.preventDefault e)
                                                                (om/refresh! owner))

                                                            (= (.-keyCode e) 40)
                                                            (do (om/update-state! owner ::selection-index inc)
                                                                (.preventDefault e)
                                                                (om/refresh! owner))

                                                            (= (.-keyCode e) 13)
                                                            (do (.preventDefault e)
                                                                (if (not (and (empty? v) (empty? ctx)))
                                                                  (let [comps (complete (or (and (::context state) ctx)
                                                                                            data) v)]
                                                                    (if (seq comps)
                                                                      (go (>! (::int-chan state)
                                                                              {::select (nth comps
                                                                                             (mod (::selection-index state)
                                                                                                  (count comps)))}))
                                                                      (when (:onKeyDown input-config)
                                                                        ((:onKeyDown input-config) e))))
                                                                  (when (:onKeyDown input-config)
                                                                    ((:onKeyDown input-config) e)))
                                                                (om/refresh! owner))))
                                             :onKeyUp #(let [si (om/get-node owner "search-input")
                                                             selection-start (.-selectionStart si)]
                                                         (om/set-state! owner ::cursor-position selection-start))
                                             :onChange #(do
                                                          (let [tv (.. % -target -value)]
                                                            (om/set-state! owner ::value tv)
                                                            (when (:onChange input-config)
                                                              ((:onChange input-config) tv))
                                                            (when tv
                                                              (go (>! (::int-chan state) tv)))))})))
                 (when-not (and (empty? v) (empty? ctx))
                   (let [completions (complete (or (and (::context state) ctx)
                                                   data) v)]
                     (when (::is-focused state)
                       (om/build completion-list-component
                                 (map #(hash-map :completion %
                                                 :context nil) completions)
                                 {:init-state {::chan (::int-chan state)}
                                  :state {::selection-index (::selection-index state)}
                                  :opts {:show-create (not (nil? create-fn))}})))))))))
