(ns camelot.component.screens
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.component.albums :as albums]
            [camelot.component.inputs :as inputs]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [camelot.util.screen :as us]
            [camelot.rest :as rest])
    (:import [goog.date DateTime]
             [goog.i18n DateTimeFormat]))

(defn load-resource-children
  "Update the state of the children defined for the selected resource type."
  [vs]
  (let [screen (us/get-screen vs)
        res (get-in screen [:sidebar :resource])
        id (get-in vs [:screen :id])
        ep (if id
             (str (get res :endpoint) "/" id)
             (get res :endpoint))]
    (rest/get-resource ep
                       #(om/update! (get vs :selected-resource)
                                    :children
                                    (:body %)))))

(defn validate
  "Check required fields against the entered data"
  [vs]
  (let [entries (get (us/get-screen vs) :schema)
        reqd (filter (fn [[k v]] (get-in v [:schema :required])) entries)
        invalid (filter #(let [data (get-in vs [:buffer (first %) :value])]
                           (or (nil? data) (= data "")
                               (and (coll? data) (empty? data)))) reqd)]
    (if (zero? (count invalid))
      (do (om/update! (get vs :selected-resource) :show-validations false)
          true)
      (do (om/update! (get vs :selected-resource) :show-validations true)
          false))))

(defn build-generator
  "Drop-down menu generator for the given view-state."
  [vs]
  (fn [template gendata gen genargs]
    (let [to-dropdown #(hash-map :vkey (get % (:vkey template))
                                 :desc (get % (:desc template)))]
      (rest/get-resource (str (get template :baseurl)
                              "/"
                              (if (= (get-in vs [:screen :mode]) :create)
                                "available"
                                "alternatives")
                              "/"
                              (get genargs :id))
                         #(om/update! gendata gen
                                      (conj (map to-dropdown (:body %))
                                            {:vkey "" :desc ""}))))))

(def generators
  "Mapping of keys to functions to generate drop-down menus."
  {:camera-statuses {:vkey :camera-status-id
                     :desc :camera-status-description
                     :baseurl "/camera-statuses"}
   :survey-sites-available {:vkey :site-id
                            :desc :site-name
                            :baseurl "/survey-sites"}
   :trap-station-session-cameras-available {:vkey :camera-id
                                            :desc :camera-name
                                            :baseurl "/trap-station-session-cameras"}})

(def events
  "Mapping of events to event functions."
  {:settings-save (fn [d] (nav/toggle-settings!) (albums/reload-albums))
   :settings-cancel #(nav/toggle-settings!)})

(defn create [success-key error-key vs resources key]
  "Create the item in the buffer and view it in readonly mode."
  (let [parent-id (us/get-parent-resource-id vs)
        basedata (deref (get vs :buffer))
        data (if parent-id
               (assoc basedata (us/get-screen-resource vs :parent-id-key)
                      {:value parent-id})
               basedata)]
    (when (validate vs)
      (nav/analytics-event "create" (us/get-resource-name vs))
      (rest/post-resource (us/get-endpoint vs) {:data data}
                          #(do
                             (load-resource-children vs)
                             (om/update! (get vs :screen) :mode :readonly)
                             (om/update! (get vs :selected-resource) :details (:body %))
                             (om/update! vs :buffer (:body %))
                             (get events success-key))))))

(defn submit-update [success-key error-key vs resources key]
  "Submit the buffer state and return to readonly mode."
  (let [cb (get events success-key)]
    (when (validate vs)
      (om/update! resources key (deref (get vs :buffer)))
      (nav/analytics-event "update" (us/get-resource-name vs))
      (rest/put-resource (us/get-url vs) {:data (deref (get vs :buffer))}
                         #(do
                            (when-not (us/settings-screen? vs)
                              (load-resource-children vs)
                              (om/update! (get vs :screen) :mode :readonly))
                            (when cb
                              (cb)))))))

(defn cancel-update [event-key vs resources key]
  "Revert the buffer state and return to readonly mode."
  (om/update! vs :buffer (deref (get resources key)))
  (om/update! (get vs :selected-resource) :show-validations false)
  (nav/analytics-event "cancel-update" (us/get-resource-name vs))
  (when-not (us/settings-screen? vs)
    (om/update! (get vs :screen) :mode :readonly))
  (let [cb (get events event-key)]
    (when cb
      (cb))))

(defn delete [success-key error-key vs resources key]
  "Delete the resource with `key'."
  (let [cb (get events success-key)]
    (nav/analytics-event "delete" (us/get-resource-name vs))
    (rest/delete-resource (us/get-url vs) {}
                          #(do (om/update! resources key {})
                               (om/update! vs :buffer {})
                               (load-resource-children vs)
                               (om/update! (get vs :screen) :mode :create)
                               (when cb
                                 (cb))))))

(def actions
  "Mapping of actions to the corresponding action function."
  {:survey-sites (fn [vs rid]
                   (nav/breadnav! (str "/#/survey-sites/" rid) (us/get-screen-title vs)))
   :trap-stations (fn [vs rid]
                    (nav/breadnav! (str "/#/trap-stations/" rid) (us/get-screen-title vs)))
   :trap-station-sessions (fn [vs rid]
                            (nav/breadnav! (str "/#/trap-station-sessions/" rid) (us/get-screen-title vs)))
   :trap-station-session-cameras (fn [vs rid]
                                   (nav/breadnav! (str "/#/trap-station-session-cameras/" rid) (us/get-screen-title vs)))
   :import-media (fn [vs rid] (js/alert "Not yet implemented."))
   :edit-mode (fn [vs rid] (om/update! (get vs :screen) :mode :update))
   :delete (fn [vs rid] (let [screen (us/get-screen vs)]
                          (when (js/confirm "Are you sure you wish to delete this?")
                            (delete (get-in screen [:states :delete :submit :success :event])
                                    (get-in screen [:states :delete :submit :error :event])
                                    vs (get vs :selected-resource) :details))))})

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
        action (get actions (keyword value))]
    (if action
      (action vs (us/get-resource-id vs))
      (prn (str "Missing action: " (name value))))))

(defn actionmenu-builder-list
  "Return the menu options formatted for on Om's build-all."
  [vs key]
  (conj (map #(hash-map
               key (:label %)
               :action (name (:action %))
               :desc (:label %))
             (get-in (us/get-screen vs) [:actionmenu :menu]))
        {key "" :action nil :desc "Actions..."}))

(defn actionmenu-component
  "Component for the Action Menu drop-down"
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (let [key :vkey]
        (apply dom/select #js {:className "actionmenu"
                               :onChange #(do
                                            (nav/analytics-event
                                             (str (us/get-resource-name vs) "-actionmenu")
                                             (.. % -target -value))
                                            (select-action-event-handler vs %))}
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
        (let [value (get-in (us/get-screen vs) [:schema (first menu-item)])]
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
                             [(first menu-item) value buf opts])))))))

(defn body-component
  "Render all fields in the body"
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (let [gens {:generator-fn (build-generator vs)
                  :generators generators
                  :generator-data (get vs :generator-data)
                  :generator-args {:id (if (= (get-in vs [:screen :mode]) :create)
                                         (us/get-parent-resource-id vs)
                                         (us/get-resource-id vs))}}]
        (apply dom/div #js {:className "section-body"}
               (om/build-all field-component
                             (map #(vector % vs (get vs :buffer)
                                           (if (= (get-in vs [:screen :mode]) :readonly)
                                             (merge gens {:disabled true})
                                             gens))
                                  (get (us/get-screen vs) :layout))))))))

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
               (dom/h4 nil (str "Update "  (us/get-screen-title view-state)))
               (dom/div nil (om/build body-component view-state))
               (om/build update-button-component k)))))

(defn resource-view-component
  "Component for Readonly Mode"
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (when (get (us/get-screen vs) :actionmenu)
                 (om/build actionmenu-component vs))
               (dom/h4 nil (us/get-screen-title vs))
               (dom/div nil (om/build body-component vs))))))

(defn resource-create-component
  "Component for Create Mode"
  [{:keys [view-state create]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil (str "Create " (us/get-screen-title view-state)))
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
                   :onClick #(do
                               (nav/analytics-event "sidebar-navigate"
                                                    (us/get-resource-name (get data :view-state)))
                               (rest/get-resource (get data :uri)
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
  (let [screen (us/get-screen vs)
        res (get-in screen [:sidebar :resource])]
    (apply dom/ul nil
           (dom/li nil (get res :title))
           (om/build-all sidebar-item-component
                         (map #(hash-map :item %
                                         :view-state vs
                                         :label (get res :label)
                                         :id (get res :id)
                                         :uri (get % :uri))
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
                                            (nav/analytics-event "sidebar-create"
                                                                 (us/get-resource-name vs))
                                            (om/update! vs :buffer {})
                                            (om/update! (get vs :screen) :mode :create))})
               (build-sidebar-item-components vs)))))

(defn build-update-component
  "Builder for a Update-Mode component"
  [vs]
  (let [screen (us/get-screen vs)]
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
  (let [screen (us/get-screen vs)
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
              screen (us/get-screen vs)]
          (dom/div nil
                   (when (get screen :sidebar)
                     (om/build sidebar-component vs))
                   (om/build content-component vs)))))))
