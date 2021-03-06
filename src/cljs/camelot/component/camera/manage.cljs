(ns camelot.component.camera.manage
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [om.dom :as dom]
            [camelot.nav :as nav]
            [camelot.translation.core :as tr]
            [clojure.string :as str]
            [cljs-time.format :as tf]))

(def ^:private day-formatter (tf/formatter "yyyy-MM-dd"))

(defn form-textarea
  [data owner {:keys [label value-key]}]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "field-label"} label)
               (dom/textarea #js {:className "field-input"
                                  :rows 3
                                  :cols 48
                                  :onChange #(om/update! data [value-key :value] (.. % -target -value))
                                  :value (get-in data [value-key :value] "")})))))

(defn form-input
  [data owner {:keys [label value-key required validator warning]}]
  (reify
    om/IRender
    (render [_]
      (let [vr (if validator (validator) true)]
        (when validator
          (om/update! data :validation-failure (not vr)))
        (dom/div nil
                 (dom/label #js {:className (str "field-label" (when required " required"))} label)
                 (dom/input #js {:className "field-input"
                                 :placeholder (str label "...")
                                 :onChange #(om/update! data [value-key :value] (.. % -target -value))
                                 :value (get-in data [value-key :value] "")})
                 (when-not vr
                   (dom/div #js {:className "validation-warning"} warning)))))))

(defn form-layout
  [data]
  [[(tr/translate :camera/camera-name.label)
    :camera-name :text-input {:required true
                              :validator (fn [] (let [v (get-in data [:data :camera-name :value])]
                                                  (not (or (nil? v) (= "" v)
                                                           (some #(= v %) (map :camera-name (:list data)))))))
                              :warning "Must not be blank or have the same name as another camera."}]
   [(tr/translate :camera/camera-make.label) :camera-make :text-input {}]
   [(tr/translate :camera/camera-model.label) :camera-model :text-input {}]
   [(tr/translate :camera/camera-notes.label) :camera-notes :textarea {}]])

(defn navigate-away
  []
  (nav/nav-up! 2))

(defn update-handler
  [data]
  (nav/analytics-event "camera-update" "submit")
  (rest/put-x (str "/cameras/" (get-in data [:camera-id :value])),
              {:data (select-keys (deref data) [:camera-name :camera-make
                                                :camera-model :camera-notes
                                                :camera-status-id])}
              navigate-away))

(defn submit-button
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "btn btn-primary"
                       :disabled (if (:validation-failure data)
                                   "disabled" "")
                       :title (when (:validation-failure data)
                                (tr/translate ::validation-failure-title))
                       :onClick (partial update-handler data)}
                  (tr/translate :words/update)))))

(defn cancel-button
  "Navigate away without saving the current form state"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "btn btn-default"
                       :onClick navigate-away}
                  (tr/translate :words/cancel)))))

(defn form-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (map #(om/build (if (= (nth % 2) :textarea)
                                 form-textarea
                                 form-input)
                               (:data data)
                               {:key (name (second %))
                                :opts (merge (nth % 3)
                                             {:label (first %)
                                              :value-key (second %)})})
                    (form-layout data))
               (dom/div #js {:className "button-container"}
                        (om/build cancel-button (:data data))
                        (om/build submit-button (:data data)))))))

(defn history-table-row
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/tr nil
              (dom/td nil (dom/a #js {:href (str "/#/" (:survey-id data)
                                                 "/deployments/"
                                                 (:trap-station-session-id data))}
                                 (:trap-station-name data)))
              (dom/td nil (tf/unparse day-formatter (:trap-station-session-start-date data)))
              (dom/td nil (let [end (:trap-station-session-end-date data)]
                            (if end
                              (tf/unparse day-formatter end)
                              (tr/translate :words/present))))))))

(defn history-table
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/table #js {:style #js {:table-layout "fixed"}
                      :className "history-table"}
                 (dom/thead nil
                            (dom/tr #js {:className "table-heading"}
                                    (dom/td nil (tr/translate ::trap-station-column))
                                    (dom/td nil (tr/translate ::trap-station-session-start-date-column))
                                    (dom/td nil (tr/translate ::trap-station-session-end-date-column))))
                 (dom/tbody nil
                            (om/build-all history-table-row data))))))

(defn history-section
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:loading? true})
    om/IDidMount
    (did-mount [_]
      (let [camera-id (get-in data [:data :camera-id :value])]
        (rest/get-x (str "/trap-station-session-cameras/camera/" camera-id)
                    #(do
                       (om/set-state! owner :history (get-in (:body %) [:trap-station-session-summaries :value]))
                       (om/set-state! owner :loading? false)))))
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
               (dom/br nil)
               (dom/h5 nil (tr/translate :words/history))
               (if (:loading? state)
                 (dom/div #js {:className "align-center"}
                          (dom/img #js {:className "spinner"
                                        :src "images/spinner.gif"
                                        :height "32"
                                        :width "32"}))
                 (om/build history-table (:history state)))))))

(defn manage-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil (let [v (get-in data [:data :camera-name :value])]
                                      (if (or (nil? v) (= v ""))
                                        (tr/translate ::update-camera)
                                        v))))
               (dom/div #js {:className "single-section"}
                        (om/build form-component data)
                        (om/build history-section data))))))
