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
        res (get-in screen [:sidebar :resource])]
    (rest/get-resource (get res :endpoint)
                       #(om/update! (get vs :selected-resource)
                                    :children
                                    (:body %)))))

(def events
  {:settings-save (fn [d] (nav/toggle-settings!) (albums/reload-albums))
   :settings-cancel #(nav/toggle-settings!)})

(defn get-endpoint
  [vs]
  (get-in (get-screen vs) [:resource :endpoint]))

(defn get-url
  [vs]
  (let [screen (get-screen vs)
        rid (get-in screen [:resource :id])
        base (get-in screen [:resource :endpoint])]
    (if (nil? rid)
      base
      (str base "/"
           (get-in vs [:selected-resource :details rid :value])))))

(defn settings-screen?
  [view-state]
  (= (get-in (get-screen view-state) [:resource :type]) :settings))

(defn create [success-key error-key vs resources key]
  (rest/put-resource (get-endpoint vs)
                     {:data (deref (get vs :buffer))}
                     #(do
                        (load-resource-children vs)
                        (om/update! vs :buffer {})
                        (get events success-key))))

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
  [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "section-body"}
             (om/build-all field-component
                           (map #(vector % (get data :screen) (get-in data [:view-state :buffer])
                                         (if (= (get-in data [:view-state :screen :mode]) :readonly)
                                           {:disabled true}
                                           {}))
                                (get-in data [:screen :layout])))))))

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
               (dom/button #js {:className "edit-btn btn btn-primary fa fa-edit fa-2x"
                                :onClick #(om/update! (get view-state :screen) :mode :update)
                                } " Edit")
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
                                  (str (get data :endpoint) "/"
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
                                                      :endpoint (get res :endpoint))
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
                              (dom/span nil (str "Unable to find mode: " (get-in view-state [:screen :mode])))))))))))
