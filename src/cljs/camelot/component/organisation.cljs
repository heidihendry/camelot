(ns camelot.component.organisation
  (:require [camelot.component.survey.create :as create]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.rest :as rest]
            [camelot.component.survey.core :as survey]
            [camelot.component.site.core :as site]
            [camelot.component.camera.core :as camera]
            [camelot.component.report.core :as report]
            [smithy.util :as util]
            [camelot.component.nav :as cnav]
            [camelot.nav :as nav]
            [camelot.translation.core :as tr])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn concept-item-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className (str "menu-item"
                                    (if (= (:concept data) (:active state)) " active" ""))
                    :onClick #(do
                                (go (>! (:active-chan state) (:concept data)))
                                (nav/analytics-event "organisation"
                                                     (str (name (:concept data)) "-click")))}
               (dom/span #js {:className "menu-item-title"}
                         (:name data))))))

(defn concept-menu-component
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
      (dom/div #js {:className "section simple-menu"}
               (om/build-all concept-item-component
                             (:menu data)
                             {:key :concept
                              :state (assoc state :active (:active data))})))))

(defn not-implemented
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div nil (tr/translate ::not-implemented))))))

(defn organisation-management-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil (tr/translate ::organisation)))
               (dom/div nil
                        (dom/div #js {:className "section-container"}
                                 (om/build concept-menu-component data))
                        (dom/div #js {:className "section-container"}
                                 (case (:active data)
                                   :survey (om/build survey/survey-menu-component (:survey data))
                                   :site (om/build site/site-menu-component (:site data))
                                   :camera (om/build camera/camera-menu-component (:camera data))
                                   :report (om/build report/menu-component (:report data))
                                   (om/build not-implemented data))))))))

(defn organisation-view-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :menu [{:concept :survey
                               :name (tr/translate ::surveys)}
                              {:concept :site
                               :name (tr/translate ::sites)}
                              {:concept :camera
                               :name (tr/translate ::cameras)}
                              {:concept :report
                               :name (tr/translate ::reports)}])
      (when (nil? (:active @data))
        (om/update! data :active :survey))
      (om/update! data :camera {})
      (om/update! data :site {})
      (om/update! data :report {})
      (rest/get-resource "/surveys"
                         #(om/update! data :survey {:list (:body %)})))

    om/IRender
    (render [_]
      (let [l (get-in data [:survey :list])]
        (cond
          (nil? l) (dom/div #js {:className "align-center"}
                            (dom/img #js {:className "spinner"
                                          :src "images/spinner.gif"
                                          :height "32"
                                          :width "32"}))

          (empty? l) (om/build create/create-survey-view-component data)

          :else (om/build organisation-management-component data))))))

(defn organisation-view
  "Render an album validation summary."
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :selected-survey nil)
      (when-not (:organisation app)
        (om/update! app :organisation {})))

    om/IRender
    (render [_]
      (when (:organisation app)
        (om/build organisation-view-component (:organisation app))))))
