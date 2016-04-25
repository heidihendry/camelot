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
  (get (get (state/app-state-cursor) :screens) (get-in vs [:screen :type])))

(def events
  {:settings-save (fn [d] (nav/toggle-settings!) (albums/reload-albums))
   :settings-cancel #(nav/toggle-settings!)})

(defn get-url
  [vs]
  (let [base (get-in (get-screen vs) [:resource :endpoint])
        rid (get vs :resource-id)]
    (if (nil? rid)
      base
      (str base "/" rid))))

(defn create [success-key error-key vs resources key]
  (rest/put-resource (get-url vs)
                     {:data (deref (get vs :buffer))}
                     (get events success-key)))

(defn save [success-key error-key vs resources key]
  (do
    (om/update! resources key (deref (get vs :buffer)))
    (rest/post-resource (get-url vs)
                        {:data (deref (get resources key))}
                        (get events success-key))))

(defn cancel [event-key vs resources key]
  (do
    (om/update! vs :buffer (deref (get resources key)))
    ((get events event-key))))

(defn field-component
  [[menu-item screen buf] owner]
  (reify
    om/IRender
    (render [_]
      (if (= (first menu-item) :label)
        (dom/h4 #js {:className "section-heading"} (second menu-item))
        (let [value (get-in screen [:schema (first menu-item)])]
          (dom/div #js {:className "field-container"}
                   (dom/label #js {:className "field-label"
                                   :title (get value :description)} (get value :label))
                   (om/build inputs/input-field
                             [(first menu-item) value buf])))))))

(defn body-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "section-body"}
             (om/build-all field-component
                           (map #(vector % (get data :screen) (get-in data [:view-state :buffer]))
                                (get-in data [:screen :layout])))))))

(defn resource-update-component
  [{:keys [screen view-state save cancel]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil (str "Update "  (get-in screen [:resource :title])))
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
               (dom/h4 nil (str "Create " (get-in screen [:resource :title])))
               (dom/div nil (om/build body-component
                                      {:view-state view-state
                                       :screen screen}))
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick create}
                                    "Create"))))))

(defn sidebar-item-component
  [data owner]
  (reify
    om/IRender
    (render [_]
            (dom/li #js {:className "sidebar-item"
                         :onClick #(do (rest/get-resource
                                        (str (get data :endpoint) "/"
                                             (get (:item data) (:id data)))
                                        (fn [resp]
                                          (om/update! (get-in data [:view-state :screen]) :mode :update)
                                          (om/update! (get data :view-state)
                                                      :buffer (:body resp)))))}
              (dom/a nil (get (:item data) (get data :label)))))))

(defn sidebar-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [res (get-in data [:screen :sidebar :resource])]
        (rest/get-resource (get res :endpoint)
                           #(om/update! (get-in data [:view-state :selected-resource])
                                        :children
                                        (:body %)))))
    om/IRender
    (render [_]
      (let [res (get-in data [:screen :sidebar :resource])]
        (dom/div #js {:className "sidebar"}
                 (dom/button #js {:className "create-record-btn btn btn-primary"
                                  ;; TODO fixme
                                  :onClick #(nav/nav! "/#/survey/create")} "+")
                 (apply dom/ul nil
                        (dom/li nil (get res :title))
                        (om/build-all sidebar-item-component
                                      (map #(hash-map :item %
                                                      :view-state (get data :view-state)
                                                      :label (get res :label)
                                                      :id (get res :id)
                                                      :endpoint (get res :endpoint))
                                           (get-in data [:view-state :selected-resource :children])))))))))

(defn build-view-component
  [type]
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (let [view-state (get-in app [:view type])
              screen (get-screen view-state)
              resource-key (get-in view-state [:screen :type])]
          (dom/div nil
                   (when (get screen :sidebar)
                     (om/build sidebar-component {:screen screen
                                                  :view-state view-state}))
                   (dom/div #js {:className "main-content"}
                            (case (get-in view-state [:screen :mode])
                              :update
                              (if (get view-state :buffer)
                                (let [rsave #(save (get-in screen [:states :update :submit :success :event])
                                                   (get-in screen [:states :update :submit :error :event])
                                                   view-state (get view-state :selected-resource) :details)
                                      rcancel #(cancel (get-in screen [:states :update :cancel :event])
                                                       view-state
                                                       (get view-state :selected-resource) :details)]
                                  (om/build resource-update-component {:screen screen
                                                                       :view-state view-state
                                                                       :save rsave
                                                                       :cancel rcancel}))
                                (dom/span nil "Loading..."))
                              :create
                              (let [rcreate #(create (get-in screen [:states :create :submit :success :event])
                                                     (get-in screen [:states :create :submit :error :event])
                                                     view-state
                                                     (get view-state :selected-resource)
                                                     :details)]
                                (om/build resource-create-component {:screen screen
                                                                     :view-state view-state
                                                                     :create rcreate}))
                              nil (dom/div nil)
                              (dom/span nil (str "Unable to find mode: " (get-in view-state [:screen :mode])))))))))))
