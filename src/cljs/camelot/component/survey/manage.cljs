(ns camelot.component.survey.manage
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.component.survey.create :as create]
            [om.dom :as dom]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.state :as state])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn action-item-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className (str "menu-item"
                                    (if (:active data) " active" ""))
                    :onClick #(do
                                (go (>! (:active-chan state) (:action data)))
                                (nav/analytics-event "survey"
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
      (dom/div #js {:className "section simple-menu"}
               (om/build-all action-item-component
                             (:menu data)
                             {:key :action
                              :init-state state})))))

(defn survey-management-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil "<This Survey>"))
               (dom/div nil
                        (dom/div #js {:className "section-container"}
                                 (om/build action-menu-component data)
                                 (dom/button #js {:className "btn btn-default view-library"
                                                  :onClick #(do (nav/analytics-event "survey"
                                                                                     "view-library-click")
                                                                (nav/nav! (str "/" (:selected-survey-id (state/app-state-cursor)) "/library")))}
                                             (dom/span #js {:className "fa fa-book"})
                                             " Survey Library"))
                        (dom/div #js {:className "section-container"}
                                 (case (:active data)
                                   :deployment nil
                                   :upload nil
                                   "")))))))
