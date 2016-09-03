(ns camelot.component.camera.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.component.util :as util]
            [camelot.component.camera.manage :as manage]
            [camelot.util.cursorise :as cursorise]
            [camelot.translation.core :as tr]
            [camelot.util.filter :as filter]))

(defn add-success-handler
  [data resp]
  (prn resp)
  (om/transact! data :list #(conj % (cursorise/decursorise (:body resp))))
  (om/update! data :new-camera-name nil))

(defn add-camera-handler
  [data]
  (rest/post-x "/cameras"
               {:data {:camera-name (:new-camera-name data)}}
               (partial add-success-handler data))
  (nav/analytics-event "org-camera" "create-click"))

(defn validate-proposed-camera
  [data]
  (not (some #(= (:new-camera-name data) %)
             (map :camera-name (:list data)))))

(defn add-camera-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [is-valid (validate-proposed-camera data)]
        (dom/form #js {:className "field-input-form"
                       :onSubmit #(.preventDefault %)}
                  (dom/input #js {:className "field-input"
                                  :placeholder (tr/translate ::new-camera-name-placeholder)
                                  :value (get-in data [:new-camera-name])
                                  :onChange #(om/update! data :new-camera-name
                                                         (.. % -target -value))})
                  (dom/input #js {:type "submit"
                                  :disabled (if is-valid "" "disabled")
                                  :title (when-not is-valid
                                           (tr/translate ::invalid-title))
                                  :className "btn btn-primary input-field-submit"
                                  :onClick #(add-camera-handler data)
                                  :value (tr/translate :words/add)}))))))

(defn camera-list-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed dynamic"
                    :onClick #(nav/nav! (str "/camera/" (:camera-id data)))}
               (dom/span #js {:className "status pull-right"}
                         (:camera-status-description data))
               (dom/span #js {:className "menu-item-title"}
                         (:camera-name data))
               (dom/span #js {:className "menu-item-description"}
                         (:camera-notes data))))))

(defn manage-view
  [data owner {:keys [camera-id]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :data nil)
      (om/update! data :list nil)
      (rest/get-x (str "/cameras/" camera-id)
                  #(do (om/update! data :data (:body %))
                       (rest/get-x "/cameras/"
                                   (fn [x]
                                     (let [others (filter (fn [v] (not= (get-in (:body %) [:camera-name :value])
                                                                        (:camera-name v))) (:body x))]
                                       (om/update! data :list others)))))))
    om/IRender
    (render [_]
      (when-not (nil? (:list data))
        (om/build manage/manage-component data)))))

(defn camera-menu-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource "/cameras"
                         #(om/update! data :list (:body %))))
    om/IRender
    (render [_]
      (when (:list data)
        (dom/div #js {:className "section"}
                 (dom/div nil
                          (dom/input #js {:className "field-input"
                                          :value (:filter data)
                                          :placeholder (tr/translate ::filter-cameras)
                                          :onChange #(om/update! data :filter (.. % -target -value))}))
                 (dom/div #js {:className "simple-menu scroll"}
                          (let [filtered (filter #(if (or (nil? (:filter data)) (empty? (:filter data)))
                                                    true
                                                    (re-matches (re-pattern (str "(?i).*" (:filter data) ".*"))
                                                                (str (:camera-name %)
                                                                     (:camera-status-description %)
                                                                     (:camera-notes %))))
                                                 (sort-by :camera-name (:list data)))]
                            (if (empty? filtered)
                              (om/build util/blank-slate-component {}
                                        {:opts {:item-name (tr/translate :words/cameras-lc)
                                                :advice (tr/translate ::blank-filter-advice)}})
                              (om/build-all camera-list-component filtered
                                            {:key :camera-id}))))
                 (dom/div #js {:className "sep"})
                 (om/build add-camera-component data)
                 (dom/button #js {:className "btn btn-default"
                                  :onClick #(do
                                              (nav/nav! "/cameras")
                                              (nav/analytics-event "org-camera" "advanced-click"))}
                             (tr/translate :words/advanced)))))))
