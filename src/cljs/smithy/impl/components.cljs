(ns smithy.impl.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [smithy.impl.inputs :as inputs]
            [smithy.util :as util])
  (:import [goog.date UtcDateTime]
           [goog.i18n DateTimeFormat]))

(defn actionmenu-item-component
  "Component for an item in the action menu."
  [{:keys [vkey action desc]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value action} desc))))

(defn select-action-event-handler
  "Handler for action menu selection events."
  [vs e]
  (let [value (.. e -target -value)
        event (str (util/get-resource-type-name vs) "-actionmenu")
        action (get-in vs [:actions-ref (keyword value)])]
    ((get-in vs [:events-ref :analytics-event]) event value)
    (if action
      (action vs (util/get-resource-id vs))
      (prn (str "Missing action: " (name value))))))

(defn actionmenu-builder-list
  "Return the menu options formatted for on Om's build-all."
  [vs key]
  (conj (map #(hash-map
               key (:label %)
               :action (name (:action %))
               :desc (:label %))
             (get-in (util/get-screen vs) [:actionmenu :menu]))
        {key "" :action nil :desc "Actions..."}))

(defn actionmenu-component
  "Component for the Action Menu drop-down"
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (when (not= (:action-menu-id  vs) "")
        (om/update! vs :action-menu-id ""))
      (let [key :vkey]
        (dom/select #js {:className "actionmenu"
                         :value (:action-menu-id vs)
                         :onChange #(do (select-action-event-handler vs %)
                                        (om/update! vs :action-menu-id (.. % -target -value)))}
               (om/build-all actionmenu-item-component
                             (actionmenu-builder-list vs key)
                             {:key key}))))))

(defn field-component
  "Component for input fields of any type."
  [[menu-item vs buf opts] owner]
  (reify
    om/IRender
    (render [_]
      (if (= (first menu-item) :label)
        (dom/h4 #js {:className "section-heading"} (second menu-item))
        (let [value (get-in (util/get-screen vs) [:schema (first menu-item)])]
          (dom/div #js {:className (if (and (get-in vs [:selected-resource :show-validations])
                                            (get-in value [:schema :required]))
                                     "field-container show-validations"
                                     "field-container")}
                   (dom/label #js {:className "field-required-label"} "Required")
                   (dom/label #js {:className "field-label"
                                   :title (get value :description)} (get value :label)
                                   (when (get-in value [:schema :required])
                                     (dom/label #js {:className "required-asterisk"} "*")))
                   (om/build inputs/input-field
                             [(first menu-item) value buf
                              (if (get-in value [:schema :detailed])
                                (merge opts {:detailed true})
                                opts)])))))))

(defn body-component
  "Render all fields in the body"
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (let [gens {:generator-fn (let [f (get-in vs [:events-ref :build-generator])]
                                  (when f
                                    (f vs)))
                  :generators (get vs :generators-ref)
                  :generator-data (get vs :generator-data)
                  :generator-args {:id (if (= (get-in vs [:screen :mode]) :create)
                                         (util/get-parent-resource-id vs)
                                         (util/get-resource-id vs))}
                  :image-resource-url "/media/photo"
                  :metadata (let [md-schema (get-in vs [:events-ref :metadata-schema])]
                              (and md-schema (md-schema)))}]
        (apply dom/div #js {:className "section-body"}
               (om/build-all field-component
                             (map #(vector % vs (get vs :buffer)
                                           (if (= (get-in vs [:screen :mode]) :readonly)
                                             (merge gens {:disabled true})
                                             gens))
                                  (get (util/get-screen vs) :layout))))))))

(defn update-button-component
  "Button-group component for finalising an update"
  [{:keys [view-state update cancel delete]}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "button-container"}
               (dom/button #js {:className "btn btn-primary fa fa-check fa-2x"
                                :onClick update}
                           " Update")
               (dom/button #js {:className "cancel-btn btn btn-default fa fa-undo fa-2x"
                                :onClick cancel}
                           " Cancel")))))

(defn resource-update-component
  "Component for Update Mode"
  [{:keys [view-state update cancel delete] :as k} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil (str "Update "  (util/get-screen-title view-state)))
               (dom/div nil (om/build body-component view-state))
               (om/build update-button-component k)))))

(defn resource-view-component
  "Component for Readonly Mode"
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (when (get (util/get-screen vs) :actionmenu)
                 (om/build actionmenu-component vs))
               (dom/h4 nil (util/get-screen-title vs))
               (dom/div nil (om/build body-component vs))))))

(defn resource-create-component
  "Component for Create Mode"
  [{:keys [view-state create]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil (str "Create " (util/get-screen-title view-state)))
               (dom/div nil (om/build body-component view-state))
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                         :onClick create}
                                    " Create"))))))

(defn sidebar-item-component
  "Component for a single navigation item in the sidebar."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/li #js {:className "sidebar-item"
                   :onClick #((get-in data [:view-state :events-ref :sidebar-item-click])
                              (get data :view-state)
                              (get data :resource-id))}
              (let [label (get (:item data) (get data :label))]
                (if (= (type label) UtcDateTime)
                  (let [df (DateTimeFormat. "yyyy-MM-dd HH:MM:ss")]
                    (dom/a nil (.format df label)))
                  (dom/a nil label)))))))

(defn build-sidebar-item-components
  "Return the built components for the sidebar items."
  [vs]
  (let [screen (util/get-screen vs)
        res (get-in screen [:sidebar :resource])]
    (apply dom/ul nil
           (dom/li nil (get res :title))
           (om/build-all sidebar-item-component
                         (map #(hash-map :item %
                                         :view-state vs
                                         :label (get res :label)
                                         :id (get res :id)
                                         :resource-id (get % (get res :id))
                                         :uri (get % :uri))
                              (get-in vs [:selected-resource :children]))))))

(defn sidebar-component
  "Component for the navigation sidebar."
  [vs owner]
  (reify
    om/IWillMount
    (will-mount [_]
      ((get-in vs [:actions-ref :load-resource-children]) vs))
    om/IRender
    (render [_]
      (dom/div #js {:className "sidebar"}
               (dom/button #js {:className "create-record-btn btn btn-primary fa fa-plus fa-2x"
                                :disabled (= (get-in vs [:screen :mode]) :create)
                                :onClick #((get-in vs [:events-ref :sidebar-create-click])
                                           vs)})
               (build-sidebar-item-components vs)))))

(defn build-update-component
  "Builder for a Update-Mode component"
  [vs]
  (if (get vs :buffer)
    (let [screen (util/get-screen vs)
          rupdate #((get-in vs [:events-ref :update])
                     (get-in screen [:states :update :submit :success :event])
                     (get-in screen [:states :update :submit :error :event])
                     vs
                     (get vs :selected-resource)
                     :details)
          rcancel #((get-in vs [:events-ref :cancel-update]) (get-in screen [:states :update :cancel :event])
                    vs
                    (get vs :selected-resource) :details)
          rdelete #((get-in vs [:events-ref :delete]) (get-in screen [:states :delete :submit :success :event])
                    (get-in screen [:states :delete :submit :error :event])
                    vs (get vs :selected-resource) :details)]
      (om/build resource-update-component {:view-state vs
                                           :update rupdate
                                           :cancel rcancel
                                           :delete rdelete}))
    (dom/span nil "Loading...")))

(defn build-readonly-component
  "Builder for a Readonly-Mode component"
  [vs]
  (do
    (if (get vs :buffer)
      (om/build resource-view-component vs)
      (dom/span nil "Loading..."))))

(defn build-create-component
  "Builder for a Create-Mode component"
  [vs]
  (let [screen (util/get-screen vs)
        create-fn #((get-in vs [:events-ref :create])
                    (get-in screen [:states :create :submit :success :event])
                    (get-in screen [:states :create :submit :error :event])
                    vs
                    (get vs :selected-resource)
                    :details)]
    (om/build resource-create-component {:view-state vs
                                         :create create-fn})))

(defn breadcrumb-item-component
  "A single segment in the breadcrumbs component."
  [{:keys [vs token label state]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/span #js {:className "breadcrumb-item"}
                (dom/a #js {:onClick #((get-in vs [:events-ref :breadcrumb-item-click])
                                       token vs state)}
                       label)))))

(defn breadcrumb-component
  "Navigation Breadcrumbs component."
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (let [hist-fn (get-in vs [:events-ref :nav-history])
            history (and hist-fn (hist-fn))]
        (when-not (empty? history)
          (dom/div #js {:className "breadcrumbs"}
                   (om/build-all breadcrumb-item-component (map #(assoc % :vs vs) history)
                                 {:key :token})))))))

(defn content-component
  "Wrap a component for the current view mode."
  [vs]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "main-content"}
               (om/build breadcrumb-component vs)
               (case (get-in vs [:screen :mode])
                 :update (build-update-component vs)
                 :readonly (build-readonly-component vs)
                 :create (build-create-component vs)
                 (dom/span nil (str "Unable to find mode: "
                                    (get-in vs [:screen :mode]))))))))

(defn build-view-component
  "Build a view component for `type', where type is a screen type."
  [type]
  (fn [app owner]
    (reify
      om/IWillMount
      (will-mount [_]
        (let [vs (get-in app [:view type])]
          (om/update! vs :screens-ref (get app :screens))
          (om/update! vs :events-ref (get app :events))
          (om/update! vs :actions-ref (get app :actions))
          (om/update! vs :generators-ref (get app :generators))))
      om/IRender
      (render [_]
        (let [vs (get-in app [:view type])
              screen (util/get-screen vs)]
          (if-let [rid (get-in vs [:screen :resource-id])]
            (if-let [f (get-in vs [:actions-ref :load-resource])]
              (f vs rid)))
          (dom/div #js {:className "main-content-container"}
                   (when (get screen :sidebar)
                     (om/build sidebar-component vs))
                   (om/build content-component vs)))))))
