(ns camelot.component.deployment.create
  (:require [om.core :as om]
            [om-datepicker.components :refer [datepicker]]
            [cljs.core.async :refer [<! chan >!]]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.nav :as nav]
            [camelot.util.trap-station :as util.ts]
            [camelot.state :as state]
            [camelot.util.cursorise :as cursorise]
            [camelot.translation.core :as tr])
  (:import [goog.date UtcDateTime]
           [goog.i18n DateTimeFormat])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn add-camera-success-handler
  [data field resp]
  (let [camera (cursorise/decursorise (:body resp))]
    (om/transact! data :cameras #(conj % camera))
    (om/update! (:new-camera-name data) field nil)
    (om/update! (get-in data [:data field]) :value (:camera-id camera))
    (om/update! (:camera-create-mode data) field false)))

(defn add-camera-handler
  [data field]
  (rest/post-x "/cameras"
               {:data {:camera-name (get-in data [:new-camera-name field])}}
               (partial add-camera-success-handler data field))
  (nav/analytics-event "deployment" "camera-create"))

(defn validate-proposed-camera
  [data field]
  (not (some #(= (get-in data [:new-camera-name field]) %)
             (map :camera-name (:all-cameras data)))))

(defn camera-select-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:camera-id data)} (:camera-name data)))))

(defn add-camera-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [is-valid (validate-proposed-camera data (:camera-id-field state))]
        (dom/form #js {:className "field-input-form"
                       :onSubmit #(.preventDefault %)}
                  (dom/input #js {:className "field-input"
                                  :autoFocus "autofocus"
                                  :placeholder (tr/translate ::new-camera-name-placeholder)
                                  :value (get-in data [:new-camera-name (:camera-id-field state)])
                                  :onChange #(om/update! (:new-camera-name data)
                                                         (:camera-id-field state)
                                                         (.. % -target -value))})
                  (if (empty? (get-in data [:new-camera-name (:camera-id-field state)]))
                    (dom/input #js {:type "submit"
                                    :className "btn btn-default input-field-submit"
                                    :onClick #(om/update! (:camera-create-mode data)
                                                          (:camera-id-field state) false)
                                    :value (tr/translate :words/cancel)})
                    (dom/input #js {:type "submit"
                                    :disabled (if is-valid "" "disabled")
                                    :title (when-not is-valid
                                             (tr/translate ::camera-invalid-title))
                                    :className "btn btn-primary input-field-submit"
                                    :onClick #(add-camera-handler data (:camera-id-field state))
                                    :value (tr/translate :words/add)})))))))

(defn camera-change-handler
  [data field event]
  (let [v (.. event -target -value)]
    (if (= v "create")
      (om/update! (:camera-create-mode data) field true)
      (om/update! (get-in data [:data field])
                  :value (.. event -target -value)))))

(defn camera-select-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :camera-create-mode {})
      (om/update! data :new-camera-name {}))
    om/IRenderState
    (render-state [_ state]
      (when (:camera-create-mode data)
        (if (or (empty? (:cameras data))
                (get-in data [:camera-create-mode (:camera-id-field state)]))
          (om/build add-camera-component data {:init-state state})
          (dom/select #js {:className "field-input"
                           :value (get-in data [:data (:camera-id-field state) :value])
                           :onChange (partial camera-change-handler data (:camera-id-field state))}
                      (om/build-all camera-select-option-component
                                    (cons {:camera-id -1
                                           :camera-name ""}
                                          (reverse (conj (into '()
                                                               (sort-by :camera-name
                                                                        (:cameras data)))
                                                         {:camera-id "create"
                                                          :camera-name (tr/translate ::create-new-camera)})))
                                    {:key :camera-id})))))))

(defn site-select-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:site-id data)} (:site-name data)))))

(defn site-change-handler
  [data owner event]
  (let [v (.. event -target -value)]
    (if (= v "create")
      (do
        (om/update! data :site-create-mode true)
        (.focus (om/get-node owner)))
      (om/update! (get-in data [:data :site-id])
                  :value (.. event -target -value)))))

(defn add-site-success-handler
  [data resp]
  (let [site (cursorise/decursorise (:body resp))]
    (om/transact! data :sites #(conj % site))
    (om/update! data :new-site-name nil)
    (om/update! (get-in data [:data :site-id]) :value (:site-id site))
    (om/update! data :site-create-mode false)))

(defn add-site-handler
  [data]
  (rest/post-x "/sites"
               {:data {:site-name (:new-site-name data)}}
               (partial add-site-success-handler data))
  (nav/analytics-event "deployment" "site-create"))

(defn validate-proposed-site
  [data]
  (not (some #(= (:new-site-name data) %)
             (map :site-name (:sites data)))))

(defn add-site-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [is-valid (validate-proposed-site data)]
        (dom/form #js {:className "field-input-form"
                       :onSubmit #(.preventDefault %)}
                  (dom/input #js {:className "field-input"
                                  :autoFocus "autofocus"
                                  :placeholder (tr/translate ::new-site-name)
                                  :value (get-in data [:new-site-name])
                                  :onChange #(om/update! data :new-site-name
                                                         (.. % -target -value))})
                  (if (empty? (:new-site-name data))
                    (dom/input #js {:type "submit"
                                    :className "btn btn-default input-field-submit"
                                    :onClick #(om/update! data :site-create-mode false)
                                    :value (tr/translate :words/cancel)})
                    (dom/input #js {:type "submit"
                                    :disabled (if is-valid "" "disabled")
                                    :title (when-not is-valid
                                             (tr/translate ::site-invalid-title))
                                    :className "btn btn-primary input-field-submit"
                                    :onClick #(add-site-handler data)
                                    :value (tr/translate :words/add)})))))))

(defn site-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (if (or (empty? (:sites data)) (:site-create-mode data))
        (om/build add-site-component data)
        (dom/select #js {:className "field-input"
                         :value (get-in data [:data :site-id :value])
                         :onChange (partial site-change-handler data owner)}
                    (om/build-all site-select-option-component
                                  (cons {:site-id -1
                                         :site-name ""}
                                        (reverse (conj (into '()
                                                             (sort-by :site-name
                                                                      (:sites data)))
                                                       {:site-id "create"
                                                        :site-name (tr/translate ::create-new-site)})))
                                  {:key :site-id}))))))

(defn validate-form
  [data]
  (and (get-in data [:trap-station-session-start-date :value])
       (and (get-in data [:trap-station-name :value])
            (not (empty? (get-in data [:trap-station-name :value]))))
       (util.ts/valid-latitude? (get-in data [:trap-station-latitude :value]))
       (util.ts/valid-longitude? (get-in data [:trap-station-longitude :value]))
       (<= (.getTime (or (get-in data [:trap-station-session-start-date :value]) (UtcDateTime.)))
           (.getTime (UtcDateTime.)))
       (get-in data [:primary-camera-id :value])
       (not= (get-in data [:secondary-camera-id :value])
             (get-in data [:primary-camera-id :value]))))

(defn deployment-form
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "single-section"}
               (dom/label #js {:className "field-label required"} (tr/translate :concepts/site))
               (om/build site-select-component data)
               (dom/label #js {:className "field-label required"} (tr/translate :concepts/trap-station))
               (dom/input #js {:className "field-input"
                               :type "text"
                               :value (get-in data [:data :trap-station-name :value])
                               :onChange #(om/update! (get-in data [:data :trap-station-name])
                                                      :value (.. % -target -value))})
               (dom/div nil
                        (dom/label #js {:className "field-label required"}
                                   (tr/translate ::start-date))
                        (dom/div #js {:className "field-details"}
                                 (om/build datepicker (get-in data [:data :trap-station-session-start-date]))))
               (when (> (.getTime (or (get-in data [:data :trap-station-session-start-date :value]) (UtcDateTime.)))
                        (.getTime (UtcDateTime.)))
                 (dom/label #js {:className "validation-warning"}
                            (tr/translate ::valdiation-future-date)))
               (dom/label #js {:className "field-label required"}
                          (tr/translate :words/latitude))
               (dom/input #js {:className "field-input"
                               :type "number"
                               :value (get-in data [:data :trap-station-latitude :value])
                               :onChange #(om/update! (get-in data [:data :trap-station-latitude])
                                                      :value
                                                      (.. % -target -value))})
               (let [v (get-in data [:data :trap-station-latitude :value])]
                 (when (and v (not (util.ts/valid-latitude? v)))
                   (dom/label #js {:className "validation-warning"}
                              (tr/translate ::invalid-latitude))))
               (dom/label #js {:className "field-label required"}
                          (tr/translate :words/longitude))
               (dom/input #js {:className "field-input"
                               :type "number"
                               :value (get-in data [:data :trap-station-longitude :value])
                               :onChange #(om/update! (get-in data [:data :trap-station-longitude])
                                                      :value
                                                      (.. % -target -value))})
               (let [v (get-in data [:data :trap-station-longitude :value])]
                 (when (and v (not (util.ts/valid-longitude? v)))
                   (dom/label #js {:className "validation-warning"}
                              (tr/translate ::invalid-longitude))))
               (dom/label #js {:className "field-label"}
                          (tr/translate :words/altitude))
               (dom/input #js {:className "field-input"
                               :type "number"
                               :value (get-in data [:data :trap-station-altitude :value])
                               :onChange #(om/update! (get-in data [:data :trap-station-altitude])
                                                      :value
                                                      (.. % -target -value))})
               (dom/label #js {:className "field-label required"}
                          (tr/translate ::primary-camera))
               (om/build camera-select-component data
                         {:init-state {:camera-id-field :primary-camera-id}})
               (dom/label #js {:className "field-label"}
                          (tr/translate ::secondary-camera))
               (om/build camera-select-component data
                         {:init-state {:camera-id-field :secondary-camera-id}})
               (let [v (get-in data [:data :secondary-camera-id :value])]
                 (when (and v (= (get-in data [:data :primary-camera-id :value]) v))
                   (dom/label #js {:className "validation-warning"}
                              (tr/translate ::validation-same-camera))))
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :disabled (if (validate-form (:data data)) "" "disabled")
                                         :title (if (validate-form (:data data)) ""
                                                    (tr/translate ::validation-failure))
                                         :onClick #(do
                                                     (nav/analytics-event "deployment"
                                                                          "cameracheck-submit")
                                                     (rest/post-x (str "/deployment/create/"
                                                                       (get-in (state/app-state-cursor)
                                                                               [:selected-survey :survey-id :value]))
                                                                  {:data (deref (:data data))}
                                                                  (fn [_]
                                                                    (nav/nav! (str "/" (get-in (state/app-state-cursor)
                                                                                               [:selected-survey :survey-id :value]))))))}
                                    (tr/translate :words/create) " "
                                    (dom/span #js {:className "btn-right-icon fa fa-chevron-right"})))))))

(defn section-containers-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className ""}
                        (om/build deployment-form data))))))

(defn view-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :page-state {:data {:trap-station-session-start-date {:value (UtcDateTime.)}
                                           :primary-camera-id {:value nil}
                                           :secondary-camera-id {:value nil}
                                           :trap-station-latitude {:value nil}
                                           :trap-station-longitude {:value nil}
                                           :trap-station-altitude {:value nil}
                                           :trap-station-name {:value nil}
                                           :site-id {:value nil}}})
      (go
        (let [c (chan)]
          (rest/get-x "/sites" #(go (>! c {:sites (:body %)})))
          (rest/get-x "/cameras/available" #(go (>! c {:cameras (:body %)})))
          (loop []
            (let [v (<! c)]
              (om/transact! data #(if (:page-state %)
                                    (assoc % :page-state (merge (:page-state %) v))
                                    (assoc % :page-state v))))
            (recur)))))
    om/IRender
    (render [_]
      (when (:page-state data)
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (tr/translate ::add-camera-trap)))
                 (dom/div nil (om/build section-containers-component (:page-state data))))))))
