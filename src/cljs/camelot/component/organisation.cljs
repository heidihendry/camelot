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
                                    (if (:active data) " active" ""))
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
              (doseq [m (:menu data)]
                (om/update! m :active (= (:concept m) r)))
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "section simple-menu"}
               (om/build-all concept-item-component
                             (:menu data)
                             {:key :concept
                              :init-state state})))))

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
  "Render an album validation summary."
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource "/surveys"
                         #(do (om/update! app :survey {:list (:body %)})
                              (when-not (:menu app)
                                (om/update! app :menu [{:concept :survey
                                                        :name (tr/translate ::surveys)
                                                        :active true}
                                                       {:concept :site
                                                        :name (tr/translate ::sites)}
                                                       {:concept :camera
                                                        :name (tr/translate ::cameras)}
                                                       {:concept :report
                                                        :name (tr/translate ::reports)}]))
                              (when-not (:active app)
                                (om/update! app :active :survey))
                              (when-not (:camera app)
                                (om/update! app :camera {}))
                              (when-not (:site app)
                                (om/update! app :site {}))
                              (om/update! app :report {}))))
    om/IRender
    (render [_]
      (when (get-in app [:survey :list])
        (if (empty? (get-in app [:survey :list]))
          (om/build create/create-survey-view-component app)
          (om/build organisation-management-component app))))))
