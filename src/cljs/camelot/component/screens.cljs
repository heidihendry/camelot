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

(defn load-resource-children
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

(def generators
  {:camera-statuses (fn [gendata gen genargs] (rest/get-resource "/camera-statuses"
                                                                 #(om/update! gendata gen (conj (map (fn [x] (hash-map :vkey (:camera-status-id x)
                                                                                                                       :desc (:camera-status-description x)))
                                                                                                     (:body %))
                                                                                                {:vkey "" :desc ""}))))
   :survey-sites-available (fn [gendata gen genargs]
                             (rest/get-resource (str "/survey-sites-available/" (get genargs :id))
                                                #(om/update! gendata gen (conj (map (fn [x] (hash-map :vkey (:site-id x)
                                                                                                      :desc (:site-name x)))
                                                                                    (:body %))
                                                                               {:vkey "" :desc ""}))))})

(def events
  {:settings-save (fn [d] (nav/toggle-settings!) (albums/reload-albums))
   :settings-cancel #(nav/toggle-settings!)})

(defn get-endpoint
  [vs]
  (get-in (get-screen vs) [:resource :endpoint]))

(defn get-resource-id
  [vs]
  (let [screen (get-screen vs)
        rid (get-in screen [:resource :id])]
    (if (nil? rid)
      nil
      (get-in vs [:selected-resource :details rid :value]))))

(defn get-parent-resource-id
  [vs]
  (get-in vs [:screen :id]))

(defn get-url
  [vs]
  (let [screen (get-screen vs)
        rid (get-resource-id vs)
        base (get-in screen [:resource :endpoint])]
    (if (nil? rid)
      base
      (str base "/" rid))))

(defn settings-screen?
  [view-state]
  (= (get-in (get-screen view-state) [:resource :type]) :settings))

(defn create [success-key error-key vs resources key]
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

(defn delete [success-key error-key vs resources key]
  (let [cb (get events success-key)]
    (rest/delete-resource (get-url vs) {}
                          #(do (om/update! resources key {})
                               (om/update! vs :buffer {})
                               (load-resource-children vs)
                               (om/update! (get vs :screen) :mode :create)
                               (when cb
                                 (cb))))))

(defn cancel-update [event-key vs resources key]
  (do
    (om/update! vs :buffer (deref (get resources key)))
    (when-not (settings-screen? vs)
      (om/update! (get vs :screen) :mode :readonly))
    (let [cb (get events event-key)]
      (when cb
        (cb)))))

(def actions
  {:survey-sites (fn [vs rid]
                   (nav/nav! (str "/#/survey-sites/" rid)))
   :edit-mode (fn [vs rid] (om/update! (get vs :screen) :mode :update))
   :delete (fn [vs rid] (let [screen (get-screen vs)]
                          (when (js/confirm "Are you sure you wish to delete this?")
                            (delete (get-in screen [:states :delete :submit :success :event])
                                    (get-in screen [:states :delete :submit :error :event])
                                    vs (get vs :selected-resource) :details))))})

(defn get-action
  [action]
  (action actions))

(defn actionmenu-item-component
  [{:keys [vkey action desc]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value action} desc))))

(defn actionmenu-component
  [vs owner]
  (reify
    om/IRender
    (render [_]
      (let [screen (get-screen vs)]
        (apply dom/select #js {:className "actionmenu"
                               :onChange #(let [value (.. % -target -value)
                                                action (get-action (keyword value))]
                                            (if action
                                              (action vs (get-resource-id vs))
                                              (prn (str "Missing action: " (name value)))))}
               (om/build-all actionmenu-item-component
                             (conj (map #(hash-map
                                          :vkey (:label %)
                                          :action (name (:action %))
                                          :desc (:label %))
                                        (get-in screen [:actionmenu :menu]))
                                   {:vkey "" :action nil :desc "Actions..."})
                                             {:key :vkey}))))))

(defn field-component
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
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [vs (:view-state data)]
        (apply dom/div #js {:className "section-body"}
               (om/build-all field-component
                             (map #(vector % (get data :screen) (get vs :buffer)
                                           (if (= (get-in vs [:screen :mode]) :readonly)
                                             {:disabled true
                                              :generators generators
                                              :generator-data (get vs :generator-data)
                                              :generator-args {:id (get-parent-resource-id vs)}}
                                             {:generators generators
                                              :generator-data (get vs :generator-data)
                                              :generator-args {:id (get-parent-resource-id vs)}}))
                                  (get-in data [:screen :layout]))))))))

(defn resource-update-component
  [{:keys [screen view-state update cancel delete]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil (str "Update "  (get-in screen [:resource :title])))
               (dom/div nil (om/build body-component
                                      {:view-state view-state
                                       :screen screen}))
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
                                      " Delete")))))))

(defn resource-view-component
  [{:keys [screen view-state]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (when (get screen :actionmenu)
                 (om/build actionmenu-component view-state))
               (dom/h4 nil (get-in screen [:resource :title]))
               (dom/div nil (om/build body-component
                                      {:view-state view-state
                                       :screen screen}))))))

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
                        (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                         :onClick create}
                                    " Create"))))))

(defn sidebar-item-component
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
              (dom/a nil (get (:item data) (get data :label)))))))

(defn sidebar-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (load-resource-children (:view-state data)))
    om/IRender
    (render [_]
      (let [res (get-in data [:screen :sidebar :resource])]
        (dom/div #js {:className "sidebar"}
                 (dom/button #js {:className "create-record-btn btn btn-primary fa fa-plus fa-2x"
                                  :disabled (= (get-in data [:view-state :screen :mode]) :create)
                                  :onClick #(let [vs (get data :view-state)]
                                              (om/update! vs :buffer {})
                                              (om/update! (get vs :screen) :mode :create))})
                 (apply dom/ul nil
                        (dom/li nil (get res :title))
                        (om/build-all sidebar-item-component
                                      (map #(hash-map :item %
                                                      :view-state (get data :view-state)
                                                      :label (get res :label)
                                                      :id (get res :id)
                                                      :specific-endpoint (get res :specific-endpoint))
                                           (get-in data [:view-state :selected-resource :children])))))))))

(defn build-view-component
  [type]
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (let [view-state (get-in app [:view type])
              screen (get-screen view-state)]
          (dom/div nil
                   (when (get screen :sidebar)
                     (om/build sidebar-component {:screen screen
                                                  :view-state view-state}))
                   (dom/div #js {:className "main-content"}
                            (dom/div #js {:className "resource-content"}
                                     (case (get-in view-state [:screen :mode])
                                       :update
                                       (if (get view-state :buffer)
                                         (let [rupdate #(submit-update (get-in screen [:states :update :submit :success :event])
                                                                       (get-in screen [:states :update :submit :error :event])
                                                                       view-state (get view-state :selected-resource) :details)
                                               rcancel #(cancel-update (get-in screen [:states :update :cancel :event])
                                                                       view-state
                                                                       (get view-state :selected-resource) :details)
                                               rdelete #(delete (get-in screen [:states :delete :submit :success :event])
                                                                (get-in screen [:states :delete :submit :error :event])
                                                                view-state (get view-state :selected-resource) :details)]
                                           (om/build resource-update-component {:screen screen
                                                                                :view-state view-state
                                                                                :update rupdate
                                                                                :cancel rcancel
                                                                                :delete rdelete}))
                                         (dom/span nil "Loading..."))
                                       :readonly
                                       (do
                                         (if (get view-state :buffer)
                                           (om/build resource-view-component {:screen screen
                                                                              :view-state view-state})
                                           (dom/span nil "Loading...")))
                                       :create
                                       (let [rcreate #(create (get-in screen [:states :create :submit :success :event])
                                                              (get-in screen [:states :create :submit :error :event])
                                                              view-state
                                                              (get view-state :selected-resource)
                                                              :details)]
                                         (om/build resource-create-component {:screen screen
                                                                              :view-state view-state
                                                                              :create rcreate}))
                                       nil (dom/div nil "")
                                       (dom/span nil (str "Unable to find mode: " (get-in view-state [:screen :mode]))))))))))))
