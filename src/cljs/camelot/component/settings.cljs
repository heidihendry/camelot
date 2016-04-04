(ns camelot.component.settings
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.reader :as reader]
            [clojure.string :as string]
            [om-datepicker.components :refer [datepicker]]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [camelot.util :refer [postreq with-baseurl]]
            [secretary.core :as secretary :refer-macros [defroute]]))

(defn set-coerced-value!
  [k]
  (fn [e data edit-key owner]
    (let [f (cond (= (type k) cljs.core/Keyword) (fn [_] (keyword (.. e -target -value)))
                  (number? k) (fn [_] (reader/read-string (.. e -target -value)))
                  :else (fn [_] (.. e -target -value)))]
      (om/transact! data edit-key f))))

(defn set-unvalidated-text! [e data edit-key owner]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn set-number! [e data edit-key owner]
  (if (re-matches #"^[\.0-9]*$" (.. e -target -value))
    (om/transact! data edit-key (fn [_] (.. e -target -value)))
    (set! (.. e -target -value) (get data edit-key))))

(defn set-percentage! [e data edit-key owner]
  (if (and (re-matches #"^[.0-9]*$" (.. e -target -value))
             (<= (reader/read-string (.. e -target -value)) 1.0))
    (om/transact! data edit-key (fn [_] (.. e -target -value)))
    (set! (.. e -target -value) (get data edit-key))))

(defn remove-item!
  [val data edit-key owner]
  (om/transact! data edit-key (fn [_] (hash-map :value (into [] (remove #{val} (get-in data [edit-key :value])))))))

(defn add-metadata-item!
  [val data edit-key owner]
  (when (not (empty? val))
    (om/transact! data edit-key (fn [_] (hash-map :value (into [] (conj (get-in data [edit-key :value]) val)))))))

(defn add-item!
  [val data edit-key owner]
  (when (not (empty? val))
    (om/transact! data edit-key (fn [_] (hash-map :value (into [] (into #{} (conj (get-in data [edit-key :value]) val))))))))

(defn save []
  (do
    (om/update! (state/app-state-cursor) :config (deref (state/config-buffer-state)))
    (postreq (with-baseurl "/settings/save")
             {:config (deref (state/config-state))}
             (fn [d] (nav/toggle-settings!)))))

(defn cancel []
  (do
    (om/update! (state/app-state-cursor) :config-buffer (deref (state/config-state)))
    (nav/toggle-settings!)))

(defn select-option-component
  [[key desc] owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (cond
                                (= (type key) cljs.core/Keyword) (name key)
                                (= (type key) cljs.core/PersistentVector) (string/join "#" (map name key))
                                :else key)} desc))))

(defmulti input-field (fn [[k v]] (:type (:schema v))))

(defmethod input-field :select
  [[k v] owner]
  (reify
    om/IRender
    (render [_]
      (let [val (get-in (state/config-buffer-state) [k :value])]
        (dom/select #js {:className "settings-input"
                         :onChange #((set-coerced-value! val) % (k (state/config-buffer-state)) :value owner)
                         :value
                         (if (= (type val) cljs.core/Keyword)
                           (name val)
                           val)}
                    (om/build-all select-option-component (:options (:schema v))))))))

(defn string-list-item
  [k]
  (fn
    [data owner]
    (reify
      om/IRender
      (render [_]
        (dom/span #js {:className "list-item"}
                  (:elt data)
                  (dom/span #js {:className "list-item-delete fa fa-trash"
                                 :onClick #(remove-item! (:elt data) (:state data) (:key data) owner)}))))))

(defn path-list-item
  [k]
  (fn [data owner]
    (reify
      om/IRender
      (render [_]
        (dom/span #js {:className "list-item"}
                  (get (state/metadata-schema-state) (:elt data))
                  (dom/span #js {:className "list-item-delete fa fa-trash"
                                 :onClick #(remove-item! (:elt data) (:state data) (:key data) owner)}))))))

(defmethod input-field :list
  [[k v s :as d] owner]
  (reify
    om/IInitState
    (init-state [_]
      {:select-value nil
       :text-value ""})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "list-input"}
               (apply dom/div nil
                      (if (= (:list-of (:schema v)) :paths)
                        (om/build-all (path-list-item k) (into [] (map #(hash-map :state s
                                                                                  :key k
                                                                                  :elt %) (get-in s [k :value]))))
                        (om/build-all (string-list-item k) (into [] (map #(hash-map :state s
                                                                                    :key k
                                                                                    :elt %)
                                                                     (sort #(< (:value %1) (:value %2)) (get-in s [k :value])))))))
               (if (= (:complete-with (:schema v)) :metadata)
                 (dom/div nil
                          (dom/select #js {:className "settings-input" :value (get state :select-value)
                                           :onChange #(om/set-state! owner :select-value (.. % -target -value))}
                                      (om/build-all select-option-component
                                                    (conj (sort #(< (second %1) (second %2)) (remove #(some (set %) (get-in s [k :value]))
                                                                                                     (state/metadata-schema-state))) [])))
                          (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                           :onClick #(do (add-metadata-item! (into [] (map keyword (string/split (get state :select-value) "#"))) s k owner)
                                                         (om/set-state! owner :select-value ""))}))
                 (dom/div nil
                          (dom/input #js {:type "text" :className "settings-input" :placeholder "Add item" :value (get state :text-value)
                                          :onKeyDown #(when (= (.-key %) "Enter")
                                                        (do (add-item! (get state :text-value) s k owner)
                                                            (om/set-state! owner :text-value "")))
                                          :onChange #(om/set-state! owner :text-value (.. % -target -value))})
                          (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                           :onClick #(do (add-item! (get state :text-value) s k owner)
                                                         (om/set-state! owner :text-value ""))})))))))

(defmethod input-field :datetime
  [[k v s :as d] owner]
  (reify
    om/IRender
    (render [_]
      (om/build datepicker (get (state/config-buffer-state) k)))))

;; TODO this needs to be between 0.0 and 1.0
(defmethod input-field :percentage
  [[k v s :as d] owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "number" :className "settings-input"
                      :onChange #(set-percentage! % (k (state/config-buffer-state)) :value owner)
                      :value (get-in (state/config-buffer-state) [k :value])}))))

(defmethod input-field :number
  [[k v s :as d] owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "number" :className "settings-input"
                      :onChange #(set-number! % (k (state/config-buffer-state)) :value owner)
                      :value (get-in (state/config-buffer-state) [k :value])}))))

(defmethod input-field :default
  [[k v s :as d] owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "text" :className "settings-input"
                      :onChange #(set-unvalidated-text! % (k (state/config-buffer-state)) :value owner)
                      :value (get-in (state/config-buffer-state) [k :value])}))))

(defn field-component
  [[menu-item s] owner]
  (reify
    om/IRender
    (render [_]
      (if (= (first menu-item) :label)
        (dom/h4 #js {:className "section-heading"} (second menu-item))
        (let [value (get (state/settings-config-state) (first menu-item))]
          (dom/div #js {:className "settings-field"}
                   (dom/label #js {:className "settings-label"
                                   :title (:description value)} (:label value))
                   (om/build input-field [(first menu-item) value s])))))))

(defn settings-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:id "settings-inner"}
             (om/build-all field-component (map #(vector % (:config-state data)) (:menu data)))))))

(defn settings-view-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil "Settings")
               (dom/div nil (om/build settings-component {:menu (:menu (:settings app))
                                                          :config-state (state/config-buffer-state)}))
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick #(save)}
                                    "Save")
                        (dom/button #js {:className "btn btn-default"
                                         :onClick #(cancel)}
                                    "Cancel"))))))
