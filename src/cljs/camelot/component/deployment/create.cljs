(ns camelot.component.deployment.create
  (:require [om.core :as om]
            [om-datepicker.components :refer [datepicker]]
            [cljs.core.async :refer [<! chan >!]]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.nav :as nav]
            [camelot.util.trap-station :as util.ts]
            [camelot.state :as state]
            [camelot.util.cursorise :as cursorise])
  (:import [goog.date UtcDateTime]
           [goog.i18n DateTimeFormat])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn camera-select-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:camera-id data)} (:camera-name data)))))

(defn camera-select-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/select #js {:className "field-input"
                       :onChange #(om/update! (get (:data data) (:camera-id-field state))
                                              :value (.. % -target -value))}
                  (om/build-all camera-select-option-component
                                (cons {:camera-id -1
                                       :camera-name ""}
                                      (sort-by :camera-name (into '() (:cameras data))))
                                {:key :camera-id})))))

(defn site-select-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:site-id data)} (:site-name data)))))

(defn site-change-handler
  [data event]
  (let [v (.. event -target -value)]
    (if (= v "create")
      (om/update! data :site-create-mode true)
      (om/update! (get-in data [:data :site-id])
                  :value (.. event -target -value)))))

(defn add-success-handler
  [data resp]
  (let [site (cursorise/decursorise (:body resp))]
    (om/transact! data :sites #(conj % site))
    (om/update! data :new-site-name nil)
    (prn "site id data")
    (prn (get-in data [:data :site-id]))
    (prn "assigning to")
    (prn (:site-id site))
    (om/update! (get-in data [:data :site-id]) :value (:site-id site))
    (om/update! data :site-create-mode false)))

(defn add-site-handler
  [data]
  (rest/post-x "/sites"
               {:data {:site-name (:new-site-name data)}}
               (partial add-success-handler data))
  (nav/analytics-event "org-site" "create-click"))

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
        (dom/form #js {:className "field-input-form"}
                  (if (empty? (:new-site-name data))
                    (dom/input #js {:type "submit"
                                    :className "btn btn-default input-field-submit"
                                    :onClick #(om/update! data :site-create-mode false)
                                    :value "Cancel"})
                    (dom/input #js {:type "submit"
                                    :disabled (if is-valid "" "disabled")
                                    :title (when-not is-valid
                                             "A site with this name already exists")
                                    :className "btn btn-primary input-field-submit"
                                    :onClick #(add-site-handler data)
                                    :value "Add"}))
                  (dom/input #js {:className "field-input"
                                  :placeholder "New site name..."
                                  :value (get-in data [:new-site-name])
                                  :onChange #(om/update! data :new-site-name
                                                         (.. % -target -value))}))))))

(defn site-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (if (or (empty? (:sites data)) (:site-create-mode data))
        (om/build add-site-component data)
        (dom/select #js {:className "field-input"
                         :value (get-in data [:data :site-id :value])
                         :onChange (partial site-change-handler data)}
                    (om/build-all site-select-option-component
                                  (cons {:site-id -1
                                         :site-name ""}
                                        (reverse (conj (into '()
                                                             (sort-by :site-name
                                                                      (:sites data)))
                                                       {:site-id "create"
                                                        :site-name "Create a new site..."})))
                                  {:key :site-id}))))))

(defn validate-form
  [data]
  (and (get-in data [:trap-station-session-start-date :value])
       (and (get-in data [:trap-station-name :value])
            (not (empty? (get-in data [:trap-station-name :value]))))
       (util.ts/valid-latitude? (get-in data [:trap-station-latitude :value]))
       (util.ts/valid-longitude? (get-in data [:trap-station-longitude :value]))
       (get-in data [:primary-camera-id :value])
       (not= (get-in data [:secondary-camera-id :value])
             (get-in data [:primary-camera-id :value]))))

(defn deployment-form
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "single-section"}
               (dom/label #js {:className "field-label required"} "Site")
               (om/build site-select-component data)
               (dom/label #js {:className "field-label required"} "Trap Station Identifier")
               (dom/input #js {:className "field-input"
                               :type "text"
                               :value (get-in data [:data :trap-station-name :value])
                               :onChange #(om/update! (get-in data [:data :trap-station-name])
                                                      :value (.. % -target -value))})
               (dom/div nil
                        (dom/label #js {:className "field-label required"} "Start Date")
                        (dom/div #js {:className "field-details"}
                                 (om/build datepicker (get-in data [:data :trap-station-session-start-date]))))
               (dom/label #js {:className "field-label required"} "Latitude")
               (dom/input #js {:className "field-input"
                               :type "number"
                               :value (get-in data [:data :trap-station-latitude :value])
                               :onChange #(om/update! (get-in data [:data :trap-station-latitude])
                                                      :value
                                                      (.. % -target -value))})
               (let [v (get-in data [:data :trap-station-latitude :value])]
                 (when (and v (not (util.ts/valid-latitude? v)))
                   (dom/label #js {:className "validation-warning"} "Latitude must be in the range [-90, 90].")))
               (dom/label #js {:className "field-label required"} "Longitude")
               (dom/input #js {:className "field-input"
                               :type "number"
                               :value (get-in data [:data :trap-station-longitude :value])
                               :onChange #(om/update! (get-in data [:data :trap-station-longitude])
                                                      :value
                                                      (.. % -target -value))})
               (let [v (get-in data [:data :trap-station-longitude :value])]
                 (prn v)
                 (when (and v (not (util.ts/valid-longitude? v)))
                   (dom/label #js {:className "validation-warning"} "Longitude must be in the range [-180, 180].")))
               (dom/label #js {:className "field-label"} "Altitude")
               (dom/input #js {:className "field-input"
                               :type "number"
                               :value (get-in data [:data :trap-station-altitude :value])
                               :onChange #(om/update! (get-in data [:data :trap-station-altitude])
                                                      :value
                                                      (.. % -target -value))})
               (dom/label #js {:className "field-label required"} "Primary Camera")
               (om/build camera-select-component data
                         {:init-state {:camera-id-field :primary-camera-id}})
               (dom/label #js {:className "field-label"} "Secondary Camera")
               (om/build camera-select-component data
                         {:init-state {:camera-id-field :secondary-camera-id}})
               (let [v (get-in data [:data :secondary-camera-id :value])]
                 (when (and v (= (get-in data [:data :primary-camera-id :value]) v))
                   (dom/label #js {:className "validation-warning"} "Secondary camera must not be the same as the primary camera.")))
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :disabled (if (validate-form (:data data)) "" "disabled")
                                         :title (if (validate-form (:data data)) ""
                                                    "Please complete all required fields and address any errors.")
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
                                    "Create "
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
                          (dom/h4 nil "Add Camera Trap"))
                 (dom/div nil (om/build section-containers-component (:page-state data))))))))
