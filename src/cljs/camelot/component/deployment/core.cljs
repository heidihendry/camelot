(ns camelot.component.deployment.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.rest :as rest]
            [om-datepicker.components :refer [datepicker]]
            [camelot.nav :as nav]
            [camelot.state :as state])
  (:import [goog.date UtcDateTime]
           [goog.i18n DateTimeFormat])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn action-item-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className (str "menu-item"
                                    (if (:active data) " active" ""))
                    :onClick #(do
                                (go (>! (:active-chan state) (:action data)))
                                (nav/analytics-event "deployment"
                                                     (str (name (:action data)) "-click")))}
               (dom/span #js {:className "menu-item-title"}
                         (:name data))))))

(defn action-menu-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:active-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [chan (om/get-state owner :active-chan)]
        (go
          (loop []
            (let [r (<! chan)]
              (om/update! data :active r)
              (doseq [m (:menu data)]
                (om/update! m :active (= (:action m) r)))
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "section simple-menu"}
               (om/build-all action-item-component
                             (:menu data)
                             {:key :action
                              :init-state state})))))

(defn camera-select-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:camera-id data)} (:camera-name data)))))

(defn camera-select-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-x (str "/trap-station-session-cameras/available/"
                       (get-in data [:trap-station-session-id :value]))
                  #(om/set-state! owner :options (:body %))))
    om/IRenderState
    (render-state [_ state]
      (dom/select #js {:className "field-input"
                       :onChange #(om/update! (get data (:camera-id-field state))
                                              :value (.. % -target -value))}
                  (om/build-all camera-select-option-component
                                (if (seq (:options state))
                                  (conj (:options state)
                                        {:camera-id -1
                                         :camera-name ""})
                                  (conj [] {:camera-id -1
                                            :camera-name "No Cameras Available"}))
                                {:key :camera-id})))))

(defn camera-status-select-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:camera-status-id data)}
                  (:camera-status-description data)))))

(defn camera-status-select-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (if (get-in data [(:camera-id-field (om/get-state owner)) :value])
        (rest/get-x (str "/camera-statuses/alternatives/"
                         (get-in data [(:camera-id-field (om/get-state owner)) :value]))
                    #(om/set-state! owner :options (:body %)))))
    om/IRenderState
    (render-state [_ state]
      (when (seq (:options state))
        (dom/select #js {:className "field-input"
                         :onChange #(om/update! (get data (:camera-status-field state))
                                                :value (.. % -target -value))}
                    (om/build-all camera-status-select-option-component
                                  (:options state)
                                  {:key :camera-status-id}))))))

(defn camera-status-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
               (dom/label #js {:className "field-label"} "Camera Status")
               (om/build camera-status-select-component data {:init-state state})
               (when (not= (js/parseInt (get-in data [(:camera-status-field state) :value])) 2)
                 (dom/div nil
                          (dom/label #js {:className "field-label"} "Replacement Camera, if any")
                          (om/build camera-select-component data {:init-state state})))))))

(defn record-camera-check-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (get-in data [:trap-station-session-end-date]))
        (om/update! data :trap-station-session-end-date {:value (UtcDateTime.)})))
    om/IRender
    (render [_]
      (when (get-in data [:trap-station-session-end-date :value])
        (dom/div #js {:className "section"}
                 (dom/div nil
                          (dom/label #js {:className "field-label"} "Camera Check Date")
                          (dom/div #js {:className "field-details"}
                                   (om/build datepicker (or (get data :trap-station-session-end-date)))))
                 (dom/h5 nil (str "Primary Camera: " ) (get-in data [:primary-camera-name :value]))
                 (om/build camera-status-component data
                           {:init-state {:camera-status-field :primary-camera-status-id
                                         :camera-id-field :primary-camera-id}})
                 (if (get-in data [:secondary-camera-id :value])
                   (dom/div nil
                            (dom/h5 nil (str "Secondary Camera: " ) (get-in data [:secondary-camera-name :value]))
                            (om/build camera-status-component data
                                      {:init-state {:camera-status-field :secondary-camera-status-id
                                                    :camera-id-field :secondary-camera-id}}))
                   (dom/div nil
                            (dom/h5 nil (str "Add a Secondary Camera"))
                            (dom/label #js {:className "field-label"} "Secondary Camera, if any")
                            (om/build camera-select-component data
                                      {:init-state {:camera-status-field :secondary-camera-status-id
                                                    :camera-id-field :secondary-camera-id}})))
                 (dom/div #js {:className "button-container"}
                          (dom/button #js {:className "btn btn-primary"
                                           :onClick #(do
                                                       (prn data)
                                                       (nav/analytics-event "deployment"
                                                                            "cameracheck-submit")
                                                       (rest/post-x "/deployment" {:data (deref data)}
                                                                    (fn [_]
                                                                      (nav/nav! (str "/" (get-in (state/app-state-cursor)
                                                                                                 [:selected-survey :survey-id :value]))))))}
                                      "Submit "
                                      (dom/span #js {:className "btn-right-icon fa fa-chevron-right"}))))))))

(defn read-only-field-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
               (dom/label #js {:className "field-label"}
                          (:label state))
               (dom/div #js {:className "field-details"}
                        (get-in data [:data (:field state) :value]))))))

(defn deployment-selected-details-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (om/build read-only-field-component data
                         {:init-state {:field :site-name
                                       :label "Site Name"}})
               (om/build read-only-field-component data
                         {:init-state {:field :trap-station-latitude
                                       :label "Latitude"}})
               (om/build read-only-field-component data
                         {:init-state {:field :trap-station-longitude
                                       :label "Longitude"}})
               (om/build read-only-field-component data
                         {:init-state {:field :trap-station-altitude
                                       :label "Altitude"}})
               (om/build read-only-field-component data
                         {:init-state {:field :primary-camera-name
                                       :label "Camera Name (Primary)"}})
               (when (:camera-name-secondary data)
                 (om/build read-only-field-component
                           {:init-state {:field :secondary-camera-name
                                         :label "Camera Name (Secondary)"}}))))))

(defn deployment-section-containers-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className "section-container"}
                        (om/build action-menu-component data))
               (dom/div #js {:className "section-container"}
                        (case (:active data)
                          :details (om/build deployment-selected-details-component data)
                          :check (om/build record-camera-check-component (:data data))
                          nil)
                        )))))

(defn deployment-view-component
  "Top-level view for deployment components."
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-x (str "/deployment/" (:page-id app))
                  #(om/update! app :page-state {:data (:body %)
                                                :menu [{:action :details
                                                        :name "Details"
                                                        :active true}
                                                       {:action :check
                                                        :name "Record Camera Check"}]
                                                :active :details})))
    om/IRender
    (render [_]
      (when (:page-state app)
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (get-in app [:page-state :data :trap-station-name :value])))
                 (dom/div nil
                          (om/build deployment-section-containers-component
                                    (:page-state app))))))))
