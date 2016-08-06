(ns smithy.impl.inputs
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [smithy.impl.state :as state]
            [clojure.string :as string]
            [om-datepicker.components :refer [datepicker]]
            [camelot.rest :as rest])
  (:import [goog.date UtcDateTime]
           [goog.i18n DateTimeFormat]))

(defn- lookup-md-desc
  [mddata md]
  (get mddata md))

(defn- path-list-item
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [rm-fn #(state/remove-item! (:elt data)
                                      (get (:state data) (:key data))
                                      :value owner)]
        (if (:disabled (:opts data))
          (dom/span #js {:className "list-item"} (lookup-md-desc (get-in data [:opts :metadata]) (:elt data)))
          (dom/span #js {:className "list-item"} (lookup-md-desc (get-in data [:opts :metadata]) (:elt data))
                    (dom/span #js {:className "list-item-delete fa fa-trash"
                                   :onClick rm-fn})))))))

(defn- list-react-key
  [key]
  (cond
    (= (type key) cljs.core/Keyword) (name key)
    (= (type key)
       cljs.core/PersistentVector) (string/join "#" (map name key))
       :else key))

(defn- string-list-item
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [rm-fn #(state/remove-item! (:elt data)
                                       (get (:state data) (:key data))
                                       :value owner)]
        (if (:disabled (:opts data))
          (dom/span #js {:className "list-item"} (:elt data))
          (dom/span #js {:className "list-item"}
                    (:elt data)
                    (dom/span #js {:className "list-item-delete fa fa-trash"
                                   :onClick rm-fn})))))))

(defn- select-option-component
  [{:keys [vkey desc]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/option
       #js {:value vkey} desc))))

(defmulti input-field (fn [[k v]] (:type (:schema v))))

(defmethod input-field :image
  [[k v buf opts]]
  (reify
    om/IRender
    (render [_]
      (let [val (get-in buf [k :value])]
        (if (nil? val)
          (dom/div nil "Preview not available")
          (dom/a #js {:href (str (:image-resource-url opts) "/" val)}
                 (dom/img #js {:className "input-field"
                               :src (str (:image-resource-url opts) "/" val "/preview")})))))))

(defmethod input-field :select
  [[k v buf opts] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get buf k))
        (om/update! buf k {:value nil}))
      (if (get-in v [:schema :get-options])
        ;; TODO fix dependency on camelot
        (rest/get-x (get-in v [:schema :get-options :url])
                    #(let [r (:body %)]
                       (om/update! (:generator-data opts)
                                   :default
                                   (vec (cons [-1 ""]
                                              (map (fn [x]
                                                     (vector (get x (get-in v [:schema :get-options :value]))
                                                             (get x (get-in v [:schema :get-options :label]))))
                                                   r))))))
        (do
          (let [generator (get-in v [:schema :generator])
                gen-template (get-in opts [:generators generator])
                generator-fn (:generator-fn opts)]
            (when (and generator generator-fn)
              (om/update! (get opts :generator-data) generator {})
              (generator-fn gen-template (get opts :generator-data) generator
                            (get opts :generator-args)))))))
    om/IWillUpdate
    (will-update [this next-props next-state]
      (let [generator (get-in v [:schema :generator])
            gen-template (get-in opts [:generators generator])
            generator-fn (:generator-fn opts)]
        (when (and generator generator-fn
                   (nil? (get-in v [:schema :get-options])))
          (om/update! (get opts :generator-data) generator {})
          (generator-fn gen-template (get opts :generator-data) generator
                        (get opts :generator-args)))))
    om/IRender
    (render [_]
      (let [val (get-in buf [k :value])
            generator (get-in v [:schema :generator])]
        (dom/select #js {:className "field-input"
                         :disabled (:disabled opts)
                         :onChange #((state/set-coerced-value! val) %
                                     (k buf) :value owner)
                         :value
                         (if (= (type val) cljs.core/Keyword)
                           (name val)
                           val)}
                    (om/build-all select-option-component
                                  (if generator
                                    (get-in opts [:generator-data generator])
                                    (map #(hash-map :vkey (list-react-key (first %))
                                                    :desc (second %))
                                         (or (get-in opts [:generator-data :default])
                                             (get-in v [:schema :options]))))
                                  {:key :vkey}))))))

(defmethod input-field :list
  [[k v buf opts :as d] owner]
  (reify
    om/IInitState
    (init-state [_]
      {:select-value nil
       :text-value ""})
    om/IWillMount
    (will-mount [_]
      (when (nil? (get buf k))
        (om/update! buf k {:value nil})))
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "list-input"}
               (apply dom/div nil
                      (if (= (:list-of (:schema v)) :paths)
                        (om/build-all path-list-item
                                      (into [] (map #(hash-map :state buf :key k :elt % :opts opts)
                                                    (sort #(< (lookup-md-desc (:metadata opts) %1) (lookup-md-desc (:metadata opts) %2))
                                                          (get-in buf [k :value])))))
                        (om/build-all string-list-item (into [] (map #(hash-map :state buf :key k :elt % :opts opts)
                                                                         (sort #(< %1 %2) (get-in buf [k :value])))))))
               (if (:disabled opts)
                 (dom/div nil "")
                 (if (= (:complete-with (:schema v)) :metadata)
                   (dom/div nil
                            (dom/select #js {:className "field-input" :value (get state :select-value)
                                             :onChange #(om/set-state! owner :select-value (.. % -target -value))}
                                        (om/build-all select-option-component
                                                      (conj (sort #(< (:value %1) (:value %2))
                                                                  (map #(hash-map :vkey (list-react-key (first %))
                                                                                  :desc (second %))
                                                                       (remove #(some (set %) (get-in buf [k :value]))
                                                                               (:metadata opts)))) {:vkey "" :desc ""})
                                                      {:key :vkey}))
                            (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                             :onClick #(do (state/add-metadata-item! (into [] (map keyword (string/split (get state :select-value) "#"))) buf k owner)
                                                           (om/set-state! owner :select-value ""))}))
                   (dom/div nil
                            (dom/input #js {:type "text" :className "field-input" :placeholder "Add item" :value (get state :text-value)
                                            :onKeyDown #(when (= (.-key %) "Enter")
                                                          (do (state/add-item! (get state :text-value) buf k owner)
                                                              (om/set-state! owner :text-value "")))
                                            :onChange #(om/set-state! owner :text-value (.. % -target -value))})
                            (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                             :onClick #(do (state/add-item! (get state :text-value) buf k owner)
                                                           (om/set-state! owner :text-value ""))}))))))))

(defmethod input-field :datetime
  [[k v buf opts :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get buf k))
        (om/update! buf k {:value (UtcDateTime.)})))
    om/IRender
    (render [_]
      (if (:disabled opts)
        (if (:detailed opts)
          (let [df (DateTimeFormat. "EEE, dd LLL yyyy")
                tf (DateTimeFormat. "HH:MM:ss")]
            (dom/div nil
                     (.format tf (get-in buf [k :value])) " on "
                     (.format df (get-in buf [k :value]))))
          (let [df (DateTimeFormat. "EEE, dd LLL yyyy")]
            (dom/div nil (.format df (get-in buf [k :value])))))
        (when (get buf k)
          (om/build datepicker (get buf k)))))))

(defmethod input-field :percentage
  [[k v buf opts :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get buf k))
        (om/update! buf k {:value nil})))
    om/IRender
    (render [_]
      (dom/input #js {:type "number" :className "field-input"
                      :step "any"
                      :disabled (:disabled opts)
                      :onChange #(state/set-percentage! % (k buf) :value owner)
                      :value (get-in buf [k :value])}))))

(defmethod input-field :number
  [[k v buf opts :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get buf k))
        (om/update! buf k {:value nil})))
    om/IRender
    (render [_]
      (dom/input #js {:type "number" :className "field-input"
                      :step "any"
                      :disabled (:disabled opts)
                      :onChange #(state/set-number! % (k buf) :value owner)
                      :value (get-in buf [k :value])}))))

(defmethod input-field :directory
  [[k v buf opts :as d] owner]
  (reify
    om/IRender
    (render [_]
      (let [schema (:schema v)]
        ;; setting the `is' property bypasses filtering performed by React,
        ;; allowing directory and webkitdirectory to be set.
        (dom/input #js {:is "special"
                        "directory" ""
                        "webkitdirectory" ""
                        :type "file"})))))

(defmethod input-field :textarea
  [[k v buf opts :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get buf k))
        (om/update! buf k {:value nil})))
    om/IRender
    (render [_]
      (let [schema (:schema v)]
        (if (:disabled opts)
          (dom/pre #js {:className "field-input"}
           (get-in buf [k :value]))
          (dom/textarea #js {:className "field-input"
                             :rows (:rows schema)
                             :cols (:cols schema)
                             :onChange #(state/set-unvalidated-text! % (k buf) :value owner)
                             :value (get-in buf [k :value])}))))))

(defmethod input-field :boolean
  [[k v buf opts :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get buf k))
        (om/update! buf k {:value nil})))
    om/IRender
    (render [_]
      (prn (get-in buf [k :value]))
      (dom/input #js {:type "checkbox" :className "field-input-checkbox"
                      :disabled (:disabled opts)
                      :onChange #(state/set-flag! % (k buf) :value owner)
                      :checked (get-in buf [k :value])}))))

(defmethod input-field :default
  [[k v buf opts :as d] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get buf k))
        (om/update! buf k {:value nil})))
    om/IRender
    (render [_]
      (dom/input #js {:type "text" :className "field-input"
                      :disabled (:disabled opts)
                      :onChange #(state/set-unvalidated-text! % (k buf) :value owner)
                      :value (get-in buf [k :value])}))))
