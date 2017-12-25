(ns camelot.component.camera.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.component.util :as util]
            [camelot.component.camera.manage :as manage]
            [camelot.util.cursorise :as cursorise]
            [camelot.translation.core :as tr]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.util.filter :as filter]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn delete
  "Delete the camera and trigger a removal event."
  [state data event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (js/confirm (tr/translate ::confirm-delete))
    (rest/delete-x (str "/cameras/" (:camera-id data))
                   #(go (>! (:chan state) {:event :delete
                                           :data data})))))

(defn add-success-handler
  [data resp]
  (om/transact! data :list #(conj % (cursorise/decursorise (:body resp))))
  (om/update! data :new-camera-name ""))

(defn add-camera-handler
  [data]
  (rest/post-x "/cameras"
               {:data {:camera-name (:new-camera-name data)}}
               (partial add-success-handler data))
  (nav/analytics-event "org-camera" "create-click"))

(defn validate-proposed-camera
  [data]
  (not (or (nil? (:new-camera-name data))
           (let [camera (-> data :new-camera-name str/trim str/lower-case)]
             (or (empty? camera)
                 (some #(= camera (-> % str/trim str/lower-case))
                       (map :camera-name (:list data))))))))

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
                                  :value (get data :new-camera-name "")
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
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item detailed dynamic"
                    :onClick #(nav/nav! (str "/camera/" (:camera-id data)))}
               (dom/div #js {:className "pull-right fa fa-times remove top-corner"
                             :onClick (partial delete state data)})
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
      (if (nil? (:list data))
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))
        (om/build manage/manage-component data)))))

(defn camera-menu-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! data :list nil))
    om/IDidMount
    (did-mount [_]
      (rest/get-resource "/cameras"
                         #(om/update! data :list (:body %)))
      (let [ch (om/get-state owner :chan)]
        (go
          (loop []
            (let [r (<! ch)]
              (cond
                (= (:event r) :delete)
                (om/transact! data :list #(remove (fn [x] (= x (:data r))) %))))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! data :list nil))
    om/IRenderState
    (render-state [_ state]
      (if (:list data)
        (dom/div #js {:className "section"}
                 (dom/div nil
                          (dom/input #js {:className "field-input"
                                          :value (get data :filter "")
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
                                        {:opts {:item-name (tr/translate ::blank-item-name)
                                                :advice (tr/translate ::blank-filter-advice)}})
                              (om/build-all camera-list-component filtered
                                            {:key :camera-id
                                             :init-state state}))))
                 (dom/div #js {:className "sep"})
                 (om/build add-camera-component data)
                 (dom/button #js {:className "btn btn-default"
                                  :onClick #(do
                                              (nav/nav! "/cameras")
                                              (nav/analytics-event "org-camera" "advanced-click"))}
                             (tr/translate :words/advanced)))
        (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"}))))))
