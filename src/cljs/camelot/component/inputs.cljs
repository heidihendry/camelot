(ns camelot.component.inputs
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.state :as state]
            [clojure.string :as string]
            [om-datepicker.components :refer [datepicker]]))

(defn- lookup-md-desc
  [md]
  (get (state/metadata-schema-state) md))

(defn- path-list-item
  [k]
  (fn [data owner]
    (reify
      om/IRender
      (render [_]
        (let [rm-fn #(state/remove-item! (:elt data)
                                         (get (:state data) (:key data))
                                         :value owner)]
          (dom/span #js {:className "list-item"} (lookup-md-desc (:elt data))
                    (dom/span #js {:className "list-item-delete fa fa-trash"
                                   :onClick rm-fn})))))))

(defn- string-list-item
  [k]
  (fn
    [data owner]
    (reify
      om/IRender
      (render [_]
        (let [rm-fn #(state/remove-item! (:elt data)
                                         (get (:state data) (:key data))
                                         :value owner)]
          (dom/span #js {:className "list-item"}
                    (:elt data)
                    (dom/span #js {:className "list-item-delete fa fa-trash"
                                   :onClick rm-fn})))))))

(defn- select-option-component
  [[key desc] owner]
  (reify
    om/IRender
    (render [_]
      (dom/option
       #js {:value (cond
                     (= (type key)
                        cljs.core/Keyword) (name key)
                     (= (type key)
                        cljs.core/PersistentVector) (string/join "#" (map name key))
                     :else key)} desc))))

(defmulti input-field (fn [[k v]] (:type (:schema v))))

(defmethod input-field :select
  [[k v s] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get s k))
        (om/update! s k {:value nil})))
    om/IRender
    (render [_]
      (let [val (get-in s [k :value])]
        (dom/select #js {:className "field-input"
                         :onChange #((state/set-coerced-value! val) %
                                     (k s) :value owner)
                         :value
                         (if (= (type val) cljs.core/Keyword)
                           (name val)
                           val)}
                    (om/build-all select-option-component (:options (:schema v))))))))

(defmethod input-field :list
  [[k v s :as d] owner]
  (reify
    om/IInitState
    (init-state [_]
      {:select-value nil
       :text-value ""})
    om/IWillMount
    (will-mount [_]
      (when (nil? (get s k))
        (om/update! s k {:value nil})))
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "list-input"}
               (apply dom/div nil
                      (if (= (:list-of (:schema v)) :paths)
                        (om/build-all (path-list-item k)
                                      (into [] (map #(hash-map :state s :key k :elt %)
                                                    (sort #(< (lookup-md-desc %1) (lookup-md-desc %2))
                                                          (get-in s [k :value])))))
                        (om/build-all (string-list-item k) (into [] (map #(hash-map :state s :key k :elt %)
                                                                         (sort #(< %1 %2) (get-in s [k :value])))))))
               (if (= (:complete-with (:schema v)) :metadata)
                 (dom/div nil
                          (dom/select #js {:className "field-input" :value (get state :select-value)
                                           :onChange #(om/set-state! owner :select-value (.. % -target -value))}
                                      (om/build-all select-option-component
                                                    (conj (sort #(< (second %1) (second %2)) (remove #(some (set %) (get-in s [k :value]))
                                                                                                     (state/metadata-schema-state))) [])))
                          (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                           :onClick #(do (state/add-metadata-item! (into [] (map keyword (string/split (get state :select-value) "#"))) s k owner)
                                                         (om/set-state! owner :select-value ""))}))
                 (dom/div nil
                          (dom/input #js {:type "text" :className "field-input" :placeholder "Add item" :value (get state :text-value)
                                          :onKeyDown #(when (= (.-key %) "Enter")
                                                        (do (state/add-item! (get state :text-value) s k owner)
                                                            (om/set-state! owner :text-value "")))
                                          :onChange #(om/set-state! owner :text-value (.. % -target -value))})
                          (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                           :onClick #(do (state/add-item! (get state :text-value) s k owner)
                                                         (om/set-state! owner :text-value ""))})))))))

(defmethod input-field :datetime
  [[k v s :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get s k))
        (om/update! s k {:value nil})))
    om/IRender
    (render [_]
      (prn s)
      (om/build datepicker (get s k)))))

(defmethod input-field :percentage
  [[k v s :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get s k))
        (om/update! s k {:value nil})))
    om/IRender
    (render [_]
      (prn s)
      (dom/input #js {:type "number" :className "field-input"
                      :onChange #(state/set-percentage! % (k s) :value owner)
                      :value (get-in s [k :value])}))))

(defmethod input-field :number
  [[k v s :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get s k))
        (om/update! s k {:value nil})))
    om/IRender
    (render [_]
      (dom/input #js {:type "number" :className "field-input"
                      :onChange #(state/set-number! % (k s) :value owner)
                      :value (get-in s [k :value])}))))

(defmethod input-field :default
  [[k v s :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get s k))
        (om/update! s k {:value nil})))
    om/IRender
    (render [_]
      (prn s)
      (dom/input #js {:type "text" :className "field-input"
                      :onChange #(state/set-unvalidated-text! % (k s) :value owner)
                      :value (get-in s [k :value])}))))
