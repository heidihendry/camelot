(ns camelot.component.deployment.camera-check
  (:require
   [om.core :as om]
   [om.dom :as dom]
   [camelot.rest :as rest]
   [om-datepicker.components :refer [datepicker]]
   [camelot.nav :as nav]
   [camelot.state :as state]
   [camelot.component.deployment.shared :as shared]
   [camelot.component.util :as util]
   [camelot.translation.core :as tr]
   [camelot.util.deployment :refer [camera-id-key camera-status-id-key camera-media-unrecoverable-key]]))

(def help-text (tr/translate ::help-text))
;; TODO should not be hard-coded
(def camera-active-status-id 2)

(defn camera-select-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:camera-id data)} (:camera-name data)))))

(defn camera-select-component
  [data owner {:keys [blank-description]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-x (str "/trap-station-session-cameras/available/"
                       (get-in data [:trap-station-session-id :value]))
                  #(om/set-state! owner :options (:body %))))
    om/IRenderState
    (render-state [_ state]
      (dom/select #js {:className "field-input"
                       :onChange #(do
                                    (when-not (nil? (:camera-is-new state))
                                      (om/update! data [:camera-is-new :value]
                                                  (:camera-is-new state)))
                                    (om/update! data
                                                [(:camera-id-field state) :value]
                                                (.. % -target -value)))}
                  (om/build-all camera-select-option-component
                                (if (seq (:options state))
                                  (conj (:options state)
                                        {:camera-id -1
                                         :camera-name (or blank-description "")})
                                  (conj [] {:camera-id -1
                                            :camera-name (tr/translate ::no-cameras)}))
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
      (let [v (get-in data [(:camera-id-field (om/get-state owner)) :value])]
        (when v
          (rest/get-x (str "/camera-statuses/alternatives/" v)
                      #(om/set-state! owner :options (:body %))))))
    om/IRenderState
    (render-state [_ state]
      (when (seq (:options state))
        (dom/select #js {:className "field-input"
                         :value (get-in data [(:camera-status-field state) :value])
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
               (dom/label #js {:className "field-label"} (tr/translate ::status-question))
               (om/build camera-status-select-component data {:init-state state})
               (when-not (= (js/parseInt (get-in data [(:camera-status-field state) :value]))
                      camera-active-status-id)
                 (dom/div nil
                          (dom/label #js {:className "field-label"} (tr/translate ::replacer-question))
                          (om/build camera-select-component data {:init-state state
                                                                  :opts {:blank-description
                                                                         (tr/translate ::no-replacement-camera)}})))))))

(defn camera-media-unrecoverable-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [unrec (get data (camera-media-unrecoverable-key (:camera-type state)))]
        (if (nil? unrec)
          (do
            (om/update! data (camera-media-unrecoverable-key (:camera-type state)) {:value false})
            (dom/span nil))
          (dom/div nil
                   (dom/label #js {:className "field-label"} (tr/translate ::media-retrieved))
                   (dom/select #js {:className "field-input"
                                    :value (get data [(camera-media-unrecoverable-key (:camera-type state)) :value])
                                    :onChange #(do (om/update! data [(camera-media-unrecoverable-key (:camera-type state)) :value]
                                                               (= (.. % -target -value) "true")))}
                               (dom/option #js {:value "false"} (tr/translate ::media-recovered))
                               (dom/option #js {:value "true"} (tr/translate ::media-not-recovered)))))))))

(defn record-camera-check-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data [:data :validation-problem] {:value false})
      (om/update! data [:data :primary-camera-original-id] (deref (get-in data [:data :primary-camera-id])))
      (om/update! data [:data :secondary-camera-original-id] (deref (get-in data [:data :secondary-camera-id]))))
    om/IRenderState
    (render-state [_ state]
      (if (:can-edit? state)
        (let [data (:data data)]
          (om/update! data :validation-problem {:value false})
          (let [sess-end (get-in data [:trap-station-session-end-date :value])]
            (dom/div #js {:className "section"}
                     (dom/div #js {:className "generic-container"}
                              (dom/div #js {:className "help-text"} help-text)
                              (dom/div nil
                                       (dom/label #js {:className "field-label"} (tr/translate ::camera-check-date))
                                       (dom/div #js {:className "field-details"}
                                                (om/build datepicker (or (get data :trap-station-session-end-date)))))
                              (when (and sess-end
                                         (< (.getTime sess-end) (.getTime (get-in data [:trap-station-session-start-date :value]))))
                                (om/update! (:validation-problem data) :value true)
                                (dom/label #js {:className "validation-warning"}
                                           (tr/translate ::date-validation-past)))
                              (when (shared/datetime-in-future? sess-end)
                                (om/update! (:validation-problem data) :value true)
                                (dom/label #js {:className "validation-warning"}
                                           (tr/translate ::date-validation-future)))
                              (dom/h5 nil
                                      (tr/translate ::primary-camera)
                                      ": "
                                      (get-in data [:primary-camera-name :value]))
                              (om/build camera-media-unrecoverable-component data {:init-state {:camera-type :primary}})
                              (om/build camera-status-component data
                                        {:init-state {:camera-status-field :primary-camera-status-id
                                                      :camera-id-field :primary-camera-id}})
                              (if (and (get-in data [:secondary-camera-id :value])
                                       (not (get-in data [:camera-is-new :value])))
                                (dom/div nil
                                         (dom/h5 nil (tr/translate ::secondary-camera) ": "
                                                 (get-in data [:secondary-camera-name :value]))
                                         (om/build camera-media-unrecoverable-component data {:init-state {:camera-type :secondary}})
                                         (om/build camera-status-component data
                                                   {:init-state {:camera-status-field :secondary-camera-status-id
                                                                 :camera-id-field :secondary-camera-id}}))
                                (dom/div nil
                                         (dom/h5 nil (tr/translate ::add-secondary-camera))
                                         (dom/label #js {:className "field-label"}
                                                    (tr/translate ::secondary-camera-label))
                                         (om/build camera-select-component data
                                                   {:init-state {:camera-status-field :secondary-camera-status-id
                                                                 :camera-id-field :secondary-camera-id
                                                                 :camera-is-new true}})))
                              (when (= (get-in data [:primary-camera-id :value])
                                       (get-in data [:secondary-camera-id :value]))
                                (om/update! (:validation-problem data) :value true)
                                (dom/label #js {:className "validation-warning"}
                                           (tr/translate ::validation-same-camera))))
                     (dom/div #js {:className "button-container"}
                              (dom/button #js {:className "btn btn-primary"
                                               :disabled (if (or (nil? sess-end)
                                                                 (get-in data [:validation-problem :value])) "disabled" "")
                                               :onClick #(do
                                                           (nav/analytics-event "deployment"
                                                                                "cameracheck-submit")
                                                           (rest/post-x "/camera-deployment" {:data (deref data)}
                                                                        (fn [_]
                                                                          (nav/nav! (str "/" (get-in (state/app-state-cursor)
                                                                                                     [:selected-survey :survey-id :value]))))))}
                                          "Submit "
                                          (dom/span #js {:className "btn-right-icon fa fa-chevron-right"}))))))
        (om/build util/blank-slate-component {}
                  {:opts {:notice (tr/translate ::finalised-trap-notice)
                          :advice (dom/div #js {:className "section-width"}
                                           (dom/p nil (tr/translate ::finalised-trap-advice)))}})))))
