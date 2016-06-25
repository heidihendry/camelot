(ns camelot.component.organisation
  (:require [camelot.component.survey.create :as create]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.rest :as rest]
            [camelot.component.survey.core :as survey])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn concept-item-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className (str "menu-item"
                                    (if (:active data) " active" ""))
                    :onClick #(do (prn "click")
                                  (prn (:concept data))
                                  (go (>! (:active-chan state) (:concept data))))}
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
              (prn "Received!")
              (prn r)
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
               (dom/div nil "Sorry, but this hasn't been developed yet")))))

(defn organisation-management-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil "Your Organisation"))
               (dom/div nil
                        (dom/div #js {:className "section-container"}
                                 (om/build concept-menu-component data))
                        (dom/div #js {:className "section-container"}
                                 (case (:active data)
                                   :survey (om/build survey/survey-menu-component data)
                                   (om/build not-implemented data))))))))

(defn organisation-view-component
  "Render an album validation summary."
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource
       "/surveys"
       #(om/update! app :survey {:list (:body %)
                                 :active :survey
                                 :menu [{:concept :survey
                                         :name "Surveys"
                                         :active true}
                                        {:concept :species
                                         :name "Species"}
                                        {:concept :site
                                         :name "Sites"}
                                        {:concept :camera
                                         :name "Cameras"}]})))
    om/IRender
    (render [_]
      (when (get-in app [:survey :list])
        (if (empty? (get-in app [:survey :list]))
          (om/build create/create-survey-view-component app)
          (om/build organisation-management-component (:survey app)))))))