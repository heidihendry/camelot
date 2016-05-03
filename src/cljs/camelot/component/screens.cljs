(ns camelot.component.screens
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.component.albums :as albums]
            [camelot.component.inputs :as inputs]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [camelot.rest :as rest])
    (:import [goog.date DateTime]
             [goog.i18n DateTimeFormat]))

(defn get-screen
  "Return the screen corresponding to the given view-state."
  [vs]
  (get (get (state/app-state-cursor) :screens) (get-in vs [:screen :type])))

(defn load-resource-children
  "Update the state of the children defined for the selected resource type."
  [vs]
  (let [screen (get-screen vs)
        res (get-in screen [:sidebar :resource])
        id (get-in vs [:screen :id])
        ep (if id
             (str (get res :listing-endpoint) "/" id)
             (get res :listing-endpoint))]
    (rest/get-resource ep
                       #(om/update! (get vs :selected-resource)
                                    :children
                                    (:body %)))))

(defn camera-status-generator
  "Drop-down menu generator for camera statuses."
  [gendata gen genargs]
  (let [to-dropdown #(hash-map :vkey (:camera-status-id %)
                               :desc (:camera-status-description %))]
    (rest/get-resource
     "/camera-statuses"
     #(om/update! gendata gen
                  (conj (map to-dropdown (:body %))
                        {:vkey "" :desc ""})))))

(defn trap-station-session-cameras-available-generator
  "Drop-down menu generator for the available survey sites."
  [gendata gen genargs]
  (let [to-dropdown #(hash-map :vkey (:camera-id %)
                               :desc (:camera-name %))]
    (rest/get-resource (str "/trap-station-session-cameras-available/" (get genargs :id))
                       #(om/update! gendata gen
                                    (conj (map to-dropdown (:body %))
                                          {:vkey "" :desc ""})))))

(defn survey-sites-available-generator
  "Drop-down menu generator for the available survey sites."
  [gendata gen genargs]
  (let [to-dropdown #(hash-map :vkey (:site-id %)
                               :desc (:site-name %))]
    (rest/get-resource (str "/survey-sites-available/" (get genargs :id))
                       #(om/update! gendata gen
                                    (conj (map to-dropdown (:body %))
                                          {:vkey "" :desc ""})))))

(def generators
  "Mapping of keys to functions to generate drop-down menus."
  {:camera-statuses camera-status-generator
   :survey-sites-available survey-sites-available-generator
   :trap-station-session-cameras-available trap-station-session-cameras-available-generator})

(def events
  "Mapping of events to event functions."
  {:settings-save (fn [d] (nav/toggle-settings!) (albums/reload-albums))
   :settings-cancel #(nav/toggle-settings!)})

(defn get-endpoint
  "Return the current resource's endpoint."
  [vs]
  (get-in (get-screen vs) [:resource :endpoint]))

(defn get-resource-id
  "Return the ID of the current resource for this view-state."
  [vs]
  (let [screen (get-screen vs)
        rid (get-in screen [:resource :id])]
    (if (nil? rid)
      nil
      (get-in vs [:selected-resource :details rid :value]))))

(defn get-parent-resource-id
  "Return the parent resource's endpoint URL for the current view-state."
  [vs]
  (get-in vs [:screen :id]))

(defn get-url
  "Return the endpoint URL for the current view-state."
  [vs]
  (let [screen (get-screen vs)
        rid (get-resource-id vs)
        base (get-in screen [:resource :endpoint])]
    (if (nil? rid)
      base
      (str base "/" rid))))

(defn settings-screen?
  "Predicate for whether this view-state is for the settings screen."
  [vs]
  (= (get-in (get-screen vs) [:resource :type]) :settings))

(defn create [success-key error-key vs resources key]
  "Create the item in the buffer and view it in readonly mode."
  (let [parent-id (get-parent-resource-id vs)
        basedata (deref (get vs :buffer))
        data (if parent-id
               (assoc basedata (get-in (get-screen vs) [:resource :parent-id-key])
                      {:value parent-id})
               basedata)]
    (rest/put-resource (get-endpoint vs)
                       {:data data}
                       #(do
                          (load-resource-children vs)
                          (om/update! (get vs :screen) :mode :readonly)
                          (get events success-key)))))

(defn submit-update [success-key error-key vs resources key]
  "Submit the buffer state and return to readonly mode."
  (let [cb (get events success-key)]
    (om/update! resources key (deref (get vs :buffer)))
    (rest/post-resource (get-endpoint vs)
                        {:data (deref (get resources key))}
                        #(do
                           (when-not (settings-screen? vs)
                             (load-resource-children vs)
                             (om/update! (get vs :screen) :mode :readonly))
                           (when cb
                             (cb))))))

(defn cancel-update [event-key vs resources key]
  "Revert the buffer state and return to readonly mode."
  (do
    (om/update! vs :buffer (deref (get resources key)))
    (when-not (settings-screen? vs)
      (om/update! (get vs :screen) :mode :readonly))
    (let [cb (get events event-key)]
      (when cb
        (cb)))))

(defn delete [success-key error-key vs resources key]
  "Delete the resource with `key'."
  (let [cb (get events success-key)]
    (rest/delete-resource (get-url vs) {}
                          #(do (om/update! resources key {})
                               (om/update! vs :buffer {})
                               (load-resource-children vs)
                               (om/update! (get vs :screen) :mode :create)
                               (when cb
                                 (cb))))))

(def actions
  "Mapping of actions to the corresponding action function."
  {:survey-sites (fn [vs rid]
                   (nav/breadnav! (str "/#/survey-sites/" rid)
                                  (let [screen (get-screen vs)]
                                             (get-in screen [:resource :title]))))
   :trap-stations (fn [vs rid]
                    (nav/breadnav! (str "/#/trap-stations/" rid)
                                   (let [screen (get-screen vs)]
                                             (get-in screen [:resource :title]))))
   :trap-station-sessions (fn [vs rid]
                            (nav/breadnav! (str "/#/trap-station-sessions/" rid)
                                           (let [screen (get-screen vs)]
                                             (get-in screen [:resource :title]))))
   :trap-station-session-cameras (fn [vs rid]
                                   (nav/breadnav! (str "/#/trap-station-session-cameras/" rid)
                                                  (let [screen (get-screen vs)]
                                             (get-in screen [:resource :title]))))
   :import-media (fn [vs rid] (js/alert "Not yet implemented."))
   :edit-mode (fn [vs rid] (om/update! (get vs :screen) :mode :update))
   :delete (fn [vs rid] (let [screen (get-screen vs)]
                          (when (js/confirm "Are you sure you wish to delete this?")
                            (delete (get-in screen [:states :delete :submit :success :event])
                                    (get-in screen [:states :delete :submit :error :event])
                                    vs (get vs :selected-resource) :details))))})

(defn get-action
  "Return the corresponding `action' function or nil if one's not found."
  [action]
  (action actions))

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
        action (get-action (keyword value))]
    (if action
      (action vs (get-resource-id vs))
      (prn (str "Missing action: " (name value))))))

(defn actionmenu-builder-list
  "Return the menu options formatted for on Om's build-all."
  [screen key]
  (conj (map #(hash-map
               key (:label %)
               :action (name (:action %))
               :desc (:label %))
             (get-in screen [:actionmenu :menu]))
        {key "" :action nil :desc "Actions..."}))

(defn actionmenu-component
  "Component for the Action Menu drop-down"
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (let [screen (get-screen vs)
            key :vkey]
        (apply dom/select #js {:className "actionmenu"
                               :onChange #(select-action-event-handler vs %)}
               (om/build-all actionmenu-item-component
                             (actionmenu-builder-list screen key)
                             {:key key}))))))

(defn field-component
  "Component for input fields of any type."
  [[menu-item screen buf opts] owner]
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
                             [(first menu-item) value buf opts])))))))

(defn body-component
  "Render all fields in the body"
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (let [screen (get-screen vs)
            gens {:generators generators
                  :generator-data (get vs :generator-data)
                  :generator-args {:id (get-parent-resource-id vs)}}]
        (apply dom/div #js {:className "section-body"}
               (om/build-all field-component
                             (map #(vector % screen (get vs :buffer)
                                           (if (= (get-in vs [:screen :mode]) :readonly)
                                             (merge gens {:disabled true})
                                             gens))
                                  (get screen :layout))))))))

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
                           " Cancel")
               (when-not (settings-screen? view-state)
                 (dom/button #js {:className "delete-btn btn btn-danger fa fa-trash fa-2x"
                                  :onClick #(when (js/confirm "Are you sure you wish to delete this?")
                                              (delete))}
                             " Delete"))))))

(defn resource-update-component
  "Component for Update Mode"
  [{:keys [view-state update cancel delete] :as k} owner]
  (reify
    om/IRender
    (render [_]
      (let [screen (get-screen view-state)]
        (dom/div nil
                 (dom/h4 nil (str "Update "  (get-in screen [:resource :title])))
                 (dom/div nil (om/build body-component view-state))
                 (om/build update-button-component k))))))

(defn resource-view-component
  "Component for Readonly Mode"
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (let [screen (get-screen vs)]
        (dom/div nil
                 (when (get screen :actionmenu)
                   (om/build actionmenu-component vs))
                 (dom/h4 nil (get-in screen [:resource :title]))
                 (dom/div nil (om/build body-component vs)))))))

(defn resource-create-component
  "Component for Create Mode"
  [{:keys [view-state create]} owner]
  (reify
    om/IRender
    (render [_]
      (let [screen (get-screen view-state)]
        (dom/div nil
                 (dom/h4 nil (str "Create " (get-in screen [:resource :title])))
                 (dom/div nil (om/build body-component view-state))
                 (dom/div #js {:className "button-container"}
                          (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                           :onClick create}
                                      " Create")))))))

(defn sidebar-item-component
  "Component for a single navigation item in the sidebar."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/li #js {:className "sidebar-item"
                   :onClick #(do (rest/get-resource
                                  (str (get data :specific-endpoint) "/"
                                       (get (:item data) (:id data)))
                                  (fn [resp]
                                    (om/update! (get-in data [:view-state :screen]) :mode :readonly)
                                    (om/update! (get-in data [:view-state :selected-resource]) :details (:body resp))
                                    (om/update! (get data :view-state)
                                                :buffer (:body resp)))))}
              (let [label (get (:item data) (get data :label))]
                (if (= (type label) DateTime)
                  (let [df (DateTimeFormat. "yyyy-MM-dd")]
                    (dom/a nil (.format df label)))
                  (dom/a nil label)))))))

(defn build-sidebar-item-components
  "Return the built components for the sidebar items."
  [vs]
  (let [screen (get-screen vs)
        res (get-in screen [:sidebar :resource])]
    (apply dom/ul nil
           (dom/li nil (get res :title))
           (om/build-all sidebar-item-component
                         (map #(hash-map :item %
                                         :view-state vs
                                         :label (get res :label)
                                         :id (get res :id)
                                         :specific-endpoint (get res :specific-endpoint))
                              (get-in vs [:selected-resource :children]))))))

(defn sidebar-component
  "Component for the navigation sidebar."
  [vs owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (load-resource-children vs))
    om/IRender
    (render [_]
      (dom/div #js {:className "sidebar"}
               (dom/button #js {:className "create-record-btn btn btn-primary fa fa-plus fa-2x"
                                :disabled (= (get-in vs [:screen :mode]) :create)
                                :onClick #(do
                                            (om/update! vs :buffer {})
                                            (om/update! (get vs :screen) :mode :create))})
               (build-sidebar-item-components vs)))))

(defn build-update-component
  "Builder for a Update-Mode component"
  [vs]
  (let [screen (get-screen vs)]
    (if (get vs :buffer)
      (let [rupdate #(submit-update (get-in screen [:states :update :submit :success :event])
                                    (get-in screen [:states :update :submit :error :event])
                                    vs (get vs :selected-resource) :details)
            rcancel #(cancel-update (get-in screen [:states :update :cancel :event])
                                    vs
                                    (get vs :selected-resource) :details)
            rdelete #(delete (get-in screen [:states :delete :submit :success :event])
                             (get-in screen [:states :delete :submit :error :event])
                             vs (get vs :selected-resource) :details)]
        (om/build resource-update-component {:view-state vs
                                             :update rupdate
                                             :cancel rcancel
                                             :delete rdelete}))
      (dom/span nil "Loading..."))))

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
  (let [screen (get-screen vs)
        create-fn #(create (get-in screen [:states :create :submit :success :event])
                           (get-in screen [:states :create :submit :error :event])
                           vs
                           (get vs :selected-resource)
                           :details)]
    (om/build resource-create-component {:view-state vs
                                         :create create-fn})))

(defn breadcrumb-item-component
  "A single segment in the breadcrumbs component."
  [{:keys [token label]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/span #js {:className "breadcrumb-item"}
                (dom/a #js {:onClick #(nav/breadnav-consume! token)} label)))))

(defn breadcrumb-component
  "Navigation Breadcrumbs component."
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (when-not (empty? (get (state/app-state-cursor) :nav-history))
        (let [history (get (state/app-state-cursor) :nav-history)]
          (dom/div #js {:className "breadcrumbs"}
                   (om/build-all breadcrumb-item-component history
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
      om/IRender
      (render [_]
        (let [vs (get-in app [:view type])
              screen (get-screen vs)]
          (dom/div nil
                   (when (get screen :sidebar)
                     (om/build sidebar-component vs))
                   (om/build content-component vs)))))))
