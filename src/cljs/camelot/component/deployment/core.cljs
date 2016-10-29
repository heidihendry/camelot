(ns camelot.component.deployment.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.rest :as rest]
            [om-datepicker.components :refer [datepicker]]
            [camelot.util.deployment :refer [camera-id-key camera-status-id-key camera-media-unrecoverable-key]]
            [camelot.nav :as nav]
            [camelot.state :as state]
            [camelot.component.deployment.create :as create]
            [cljs-time.format :as tf]
            [camelot.component.deployment.shared :as shared]
            [camelot.component.util :as util]
            [camelot.translation.core :as tr])
  (:import [goog.date UtcDateTime]
           [goog.i18n DateTimeFormat])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def help-text (tr/translate ::help-text))
(def ^:private day-formatter (tf/formatter "yyyy-MM-dd"))

(defn delete
  "Delete the trap station and trigger a removal event."
  [state data event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (js/confirm (tr/translate ::confirm-delete))
    (rest/delete-x (str "/trap-stations/" (:trap-station-id data))
                   #(go (>! (:chan state) {:event :delete
                                           :data data})))))

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
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (om/build-all action-item-component
                                      (:menu data)
                                      {:key :action
                                       :init-state state}))))))

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
                                      (om/update! data [:camera-is-new :value] (:camera-is-new state)))
                                    (om/update! (get data (:camera-id-field state))
                                                :value (.. % -target -value)))}
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
               (when (not= (js/parseInt (get-in data [(:camera-status-field state) :value])) 2)
                 (dom/div nil
                          (dom/label #js {:className "field-label"} (tr/translate ::replacer-question))
                          (om/build camera-select-component data {:init-state state
                                                                  :opts {:blank-description "No replacement camera"}})))))))

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

(defn can-edit?
  [data]
  (-> data :data :trap-station-session-end-date :value nil?))

(defn record-camera-check-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data [:data :validation-problem] {:value false}))
    om/IRender
    (render [_]
      (if (can-edit? data)
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
                              (when (and sess-end (> (.getTime sess-end) (.getTime (UtcDateTime.))))
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
                                                           (rest/post-x "/deployment" {:data (deref data)}
                                                                        (fn [_]
                                                                          (nav/nav! (str "/" (get-in (state/app-state-cursor)
                                                                                                     [:selected-survey :survey-id :value]))))))}
                                          "Submit "
                                          (dom/span #js {:className "btn-right-icon fa fa-chevron-right"}))))))
        (om/build util/blank-slate-component {}
                  {:opts {:notice (tr/translate ::finalised-trap-notice)
                          :advice (dom/div #js {:className "section-width"}
                                           (dom/p nil (tr/translate ::finalised-trap-advice)))}})))))

(defn read-only-field-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "field-details-item-container"}
               (dom/label #js {:className "field-label"}
                          (:label state))
               (dom/div #js {:className "field-details"}
                        (if (= (type (:field state)) cljs.core/Keyword)
                          (get-in data [:data (:field state) :value])
                          (:field state)))))))

(defn deployment-selected-details-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "generic-container simple-menu"}
                        (om/build read-only-field-component data
                                  {:init-state {:field :site-name
                                                :label (tr/translate :site/site-name.label)}})
                        (om/build read-only-field-component data
                                  {:init-state {:field (tf/unparse day-formatter (get-in data [:data :trap-station-session-start-date :value]))
                                                :label (tr/translate :trap-station-session/trap-station-session-start-date.label)}})
                        (om/build read-only-field-component data
                                  {:init-state {:field :trap-station-latitude
                                                :label (tr/translate :trap-station/trap-station-latitude.label)}})
                        (om/build read-only-field-component data
                                  {:init-state {:field :trap-station-longitude
                                                :label (tr/translate :trap-station/trap-station-longitude.label)}})
                        (om/build read-only-field-component data
                                  {:init-state {:field :primary-camera-name
                                                :label (tr/translate ::primary-camera-name)}})
                        (when (get-in data [:data :secondary-camera-name :value])
                          (om/build read-only-field-component data
                                    {:init-state {:field :secondary-camera-name
                                                  :label (tr/translate ::secondary-camera-name)}}))
                        (when (get-in data [:data :trap-station-altitude :value])
                          (om/build read-only-field-component data
                                    {:init-state {:field :trap-station-altitude
                                                  :label (tr/translate :trap-station/trap-station-altitude.label)}}))
                        (when (get-in data [:data :trap-station-distance-above-ground :value])
                          (om/build read-only-field-component data
                                    {:init-state {:field :trap-station-distance-above-ground
                                                  :label (tr/translate :trap-station/trap-station-distance-above-ground.label)}}))
                        (when (get-in data [:data :trap-station-distance-to-road :value])
                          (om/build read-only-field-component data
                                    {:init-state {:field :trap-station-distance-to-road
                                                  :label (tr/translate :trap-station/trap-station-distance-to-road.label)}}))
                        (when (get-in data [:data :trap-station-distance-to-river :value])
                          (om/build read-only-field-component data
                                    {:init-state {:field :trap-station-distance-to-river
                                                  :label (tr/translate :trap-station/trap-station-distance-to-river.label)}}))
                        (when (get-in data [:data :trap-station-distance-to-settlement :value])
                          (om/build read-only-field-component data
                                    {:init-state {:field :trap-station-distance-to-settlement
                                                  :label (tr/translate :trap-station/trap-station-distance-to-settlement.label)}}))
                        (om/build read-only-field-component data
                                  {:init-state {:field :trap-station-notes
                                                :label (tr/translate :trap-station/trap-station-notes.label)}}))
               (dom/div #js {:className "sep"})
               (dom/button #js {:className "btn btn-primary"
                                :onClick #(nav/nav!
                                           (nav/survey-url "deployments"
                                                           (get-in data [:data :trap-station-session-id :value])
                                                           "edit"))}
                           (tr/translate :words/edit))
               (dom/button #js {:className "btn btn-default"
                                :onClick #(do
                                            (nav/nav! (str "/trap-station-sessions/"
                                                           (get-in data [:data :trap-station-id :value])))
                                            (nav/analytics-event "org-survey" "advanced-click"))}
                           (tr/translate :words/advanced))))))

(defn deployment-section-containers-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when-not (can-edit? data)
        (om/update! data :active :details)
        (dorun (map #(om/update! % :active (= (:action %) :details)) (:menu data)))))
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className "section-container"}
                        (om/build action-menu-component data))
               (dom/div #js {:className "section-container"}
                        (case (:active data)
                          :details (om/build deployment-selected-details-component data)
                          :check (om/build record-camera-check-component data)
                          nil))))))

(defn edit-view-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build create/edit-component app))))

(defn create-view-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build create/view-component app))))

(defn deployment-view-component
  "Top-level view for deployment components."
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :page-state nil))
    om/IDidMount
    (did-mount [_]
      (rest/get-x (str "/deployment/" (:page-id app))
                  #(om/update! app :page-state {:data (merge (:body %)
                                                             {:validation-problem {:value false}})
                                                :menu [{:action :check
                                                        :name (tr/translate ::record-camera-check)
                                                        :active true}
                                                       {:action :details
                                                        :name (tr/translate :words/details)}]
                                                :active :check})))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! app :page-state nil))
    om/IRender
    (render [_]
      (if (:page-state app)
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "back-button-container"}
                          (dom/button #js {:className "btn btn-default back"
                                           :onClick #(nav/nav-up! 2)}
                                      (dom/span #js {:className "fa fa-mail-reply"})
                                      " " (tr/translate :words/back)))
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (get-in app [:page-state :data :trap-station-name :value])))
                 (dom/div nil
                          (om/build deployment-section-containers-component (:page-state app))))
        (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"}))))))

(defn deployment-list-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item detailed dynamic"
                    :onClick #(do
                                (nav/analytics-event "survey-deployment" "trap-station-click")
                                (nav/nav! (nav/survey-url "deployments"
                                                          (:trap-station-session-id data))))}
               (dom/div #js {:className "pull-right fa fa-times remove top-corner"
                             :onClick (partial delete state data)})
               (when (:trap-station-session-end-date data)
                 (dom/span #js {:className "status pull-right"}
                           (tr/translate ::finalised)))
               (dom/span #js {:className "menu-item-title"}
                         (:trap-station-name data))
               (dom/span #js {:className "menu-item-description"}
                         (dom/label nil (tr/translate :trap-station/trap-station-latitude.label) ":")
                         " " (:trap-station-latitude data)
                         ", "
                         (dom/label nil (tr/translate :trap-station/trap-station-longitude.label) ":")
                         " " (:trap-station-longitude data))
               (dom/div #js {:className "menu-item-description"}
                         (dom/label nil (tr/translate ::start-date) ":")
                         " " (tf/unparse day-formatter (:trap-station-session-start-date data)))
               (when (:trap-station-session-end-date data)
                 (dom/div #js {:className "menu-item-description"}
                          (dom/label nil (tr/translate ::end-date) ":")
                          " " (tf/unparse day-formatter (:trap-station-session-end-date data))))))))

(defn deployment-list-section-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! data :trap-stations nil))
    om/IDidMount
    (did-mount [_]
      (om/update! data :deployment-sort-order :trap-station-session-start-date)
      (rest/get-resource (str "/deployment/survey/"
                              (get-in (state/app-state-cursor)
                                      [:selected-survey :survey-id :value]))
                         #(om/update! data :trap-stations (:body %)))
      (let [ch (om/get-state owner :chan)]
        (go
          (loop []
            (let [r (<! ch)]
              (cond
                (= (:event r) :delete)
                (om/transact! data :trap-stations #(remove (fn [x] (= x (:data r))) %))))
            (recur)))))
    om/IRenderState
    (render-state [_ state]
      (if (:trap-stations data)
        (dom/div #js {:className "section"}
                 (dom/div #js {:className "simple-menu"}
                          (if (empty? (:trap-stations data))
                            (om/build util/blank-slate-component {}
                                      {:opts {:item-name (tr/translate ::blank-item-name-lc)
                                              :advice
                                              (dom/div nil
                                                       (dom/p nil (tr/translate ::advice-context))
                                                       (dom/p nil (tr/translate ::advice-direction)))}})
                            (dom/div nil
                                     (om/build shared/deployment-sort-menu data
                                               {:opts {:show-end-date true}})
                                     (om/build-all deployment-list-component
                                                   (sort (shared/deployment-sorters (get data :deployment-sort-order))
                                                         (:trap-stations data))
                                                   {:key :trap-station-session-id
                                                    :init-state state}))))
                 (dom/div #js {:className "sep"})
                 (dom/button #js {:className "btn btn-primary"
                                  :onClick #(do (nav/nav! (str "/"
                                                               (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])
                                                               "/deployments/create"))
                                                (nav/analytics-event "survey-deployment" "create-click"))
                                  :title (tr/translate ::create-title)}
                             (dom/span #js {:className "fa fa-plus"})
                             " " (tr/translate ::create-button)))
        (do
          (om/update! data :deployment-sort-order :trap-station-session-start-date)
          (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"})))))))
