(ns camelot.component.screens
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.component.albums :as albums]
            [camelot.component.inputs :as inputs]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [camelot.rest :as rest]))

(defn get-screen
  [vs]
  (get (:screens (state/app-state-cursor)) (get-in vs [:screen :type])))

(def events
  {:settings-save (fn [d] (nav/toggle-settings!) (albums/reload-albums))
   :settings-cancel #(nav/toggle-settings!)})

(defn get-url
  [vs]
  (let [base (get-in (get-screen vs) [:resource :endpoint])
        rid (:resource-id vs)]
    (if (nil? rid)
      base
      (str base "/" rid))))

(defn create [success-key error-key vs resources key]
  (do
    (om/update! resources key (deref (:buffer vs)))
    (rest/put-resource (get-url vs)
                        {:data (deref (get resources key))}
                        (get events success-key))))

(defn save [success-key error-key vs resources key]
  (do
    (om/update! resources key (deref (:buffer vs)))
    (rest/post-resource (get-url vs)
                        {:data (deref (get resources key))}
                        (get events success-key))))

(defn cancel [event-key vs resources key]
  (do
    (om/update! vs :buffer (get resources key))
    ((get events event-key))))

(defn field-component
  [screen]
  (fn [[menu-item s] owner]
    (reify
      om/IRender
      (render [_]
        (if (= (first menu-item) :label)
          (dom/h4 #js {:className "section-heading"} (second menu-item))
          (let [value (get-in screen [:schema (first menu-item)])]
            (dom/div #js {:className "field-container"}
                     (dom/label #js {:className "field-label"
                                     :title (:description value)} (:label value))
                     (om/build inputs/input-field
                               [(first menu-item) value s]))))))))

(defn body-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "section-body"}
             (om/build-all (field-component (:screen data))
                           (map #(vector % (:buffer (:view-state data)))
                                (:layout (:screen data))))))))

(defn resource-update-component
  [{:keys [screen view-state save cancel]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil (:title screen))
               (dom/div nil (om/build body-component
                                      {:view-state view-state
                                       :screen screen}))
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick save}
                                    "Save")
                        (dom/button #js {:className "btn btn-default"
                                         :onClick cancel}
                                    "Cancel"))))))

(defn resource-create-component
  [{:keys [screen view-state create]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil (:title screen))
               (dom/div nil (om/build body-component
                                      {:view-state view-state
                                       :screen screen}))
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick create}
                                    "Create"))))))

(defn build-view-component
  [area resources key]
  (fn [app owner]
    (reify
      om/IWillMount
      (will-mount [_]
        (let [view-state (get (:view app) area)]
          (om/update! view-state :buffer (deref (get resources key)))))
      om/IRender
      (render [_]
        (let [view-state (get (:view app) area)
              screen (get-screen view-state)]
          (case (get-in view-state [:screen :mode])
            :update
            (if (:buffer view-state)
              (let [rsave #(save (get-in screen [:states :update :submit :success :event])
                                 (get-in screen [:states :update :submit :error :event])
                                 view-state resources key)
                    rcancel #(cancel (get-in screen [:states :update :cancel :event])
                                    view-state resources key)]
                (om/build resource-update-component {:screen screen
                                                     :view-state view-state
                                                     :save rsave
                                                     :cancel rcancel}))
              (dom/span nil "Loading..."))
            :create
            (let [rcreate #(create (get-in screen [:states :create :submit :success :event])
                                   (get-in screen [:states :create :submit :error :event])
                                   view-state resources key)]
              (om/build resource-create-component {:screen screen
                                                   :view-state view-state
                                                   :create create}))))))))
