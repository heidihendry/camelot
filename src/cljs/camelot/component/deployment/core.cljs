(ns camelot.component.deployment.core
  (:require
   [om.core :as om]
   [om.dom :as dom]
   [cljs.core.async :refer [<! chan >!]]
   [camelot.rest :as rest]
   [camelot.nav :as nav]
   [camelot.state :as state]
   [cljs-time.format :as tf]
   [camelot.component.deployment.create :as create]
   [camelot.component.deployment.camera-check :as camera-check]
   [camelot.component.deployment.shared :as shared]
   [camelot.component.util :as util]
   [camelot.translation.core :as tr])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [camelot.macros.ui.deployment :as m]))

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
  "Entry in the page navigation menu."
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className (str "menu-item"
                                    (if (= (:action data) (:active state)) " active" ""))
                    :onClick #(do
                                (go (>! (:active-chan state) (:action data)))
                                (nav/analytics-event "deployment"
                                                     (str (name (:action data)) "-click")))}
               (dom/span #js {:className "menu-item-title"}
                         (:name data))))))

(defn action-menu-component
  "Page navigation menu component."
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
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (om/build-all action-item-component
                                      (:menu data)
                                      {:key :action
                                       :state (assoc state :active (:active data))}))))))

(defn read-only-field-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "field-details-item-container"}
               (dom/label #js {:className "field-label"}
                          (:label state))
               (dom/div #js {:className "field-details"}
                        (if (:field state)
                          (get-in data [:data (:field state) :value])
                          (:value state)))))))

(defn deployment-selected-details-component
  "Component for read-only details about the currently selected deployment."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (m/with-builders (dom/div #js {:className "generic-container simple-menu"})
                 [om/build read-only-field-component tr/translate data]
                 (m/build-read-only-field :site-name :site/site-name.label)
                 (m/build-read-only-calculated-field :trap-station-session-start-date
                                                     :trap-station-session/trap-station-session-start-date.label
                                                     (partial tf/unparse day-formatter))
                 (m/build-read-only-field :trap-station-latitude :trap-station/trap-station-latitude.label)
                 (m/build-read-only-field :trap-station-longitude :trap-station/trap-station-longitude.label)
                 (m/build-read-only-field :primary-camera-name ::primary-camera-name)
                 (m/build-read-only-field :secondary-camera-name ::secondary-camera-name)
                 (m/build-read-only-field :trap-station-altitude :trap-station/trap-station-altitude.label)
                 (m/build-read-only-field :trap-station-distance-above-ground :trap-station/trap-station-distance-above-ground.label)
                 (m/build-read-only-field :trap-station-distance-to-road :trap-station/trap-station-distance-to-road.label)
                 (m/build-read-only-field :trap-station-distance-to-river :trap-station/trap-station-distance-to-river.label)
                 (m/build-read-only-field :trap-station-distance-to-settlement :trap-station/trap-station-distance-to-settlement.label)
                 (m/build-read-only-field :trap-station-notes :trap-station/trap-station-notes.label))
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
    om/IInitState
    (init-state [_]
      {:can-edit? (shared/can-edit? data)})
    om/IWillMount
    (will-mount [_]
      (when-not (shared/can-edit? data)
        (om/update! data :active :details)))
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
               (dom/div #js {:className "section-container"}
                        (om/build action-menu-component data))
               (dom/div #js {:className "section-container"}
                        (case (:active data)
                          :details (om/build deployment-selected-details-component data)
                          :check (om/build camera-check/record-camera-check-component data
                                           {:state state})
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
                                                        :name (tr/translate ::record-camera-check)}
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
