(ns camelot.component.species.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [camelot.component.species.update :as update]
            [camelot.component.species.manage :as manage]
            [camelot.component.util :as util]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.translation.core :as tr])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn delete
  [state data event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (js/confirm (tr/translate ::confirm-delete))
    (rest/delete-x (str "/taxonomy/" (:taxonomy-id data) "/survey/" (state/get-survey-id))
                   #(go (>! (:chan state) {:event :remove
                                           :data data})))))

(defn species-list-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item detailed"
                    :onClick #(nav/nav! (str "/taxonomy/" (:taxonomy-id data)))}
               (dom/div #js {:className "pull-right fa fa-times remove top-corner"
                             :onClick (partial delete state data)})
               (dom/span #js {:className "menu-item-title"}
                         (:taxonomy-label data))
               (dom/span #js {:className "menu-item-description"}
                         (when (:taxonomy-common-name data)
                           (dom/div nil
                                    (dom/label nil (tr/translate :taxonomy/taxonomy-common-name.label)) ":"
                                    " "
                                    (:taxonomy-common-name data)))
                         (dom/span nil
                                   (when (:taxonomy-order data)
                                     (dom/span nil
                                               (dom/label nil (tr/translate :taxonomy/taxonomy-order.label) ":")
                                               " "
                                               (:taxonomy-order data) " "))
                                   (when (:taxonomy-family data)
                                     (dom/span nil
                                               (dom/label nil (tr/translate :taxonomy/taxonomy-family.label) ":")
                                               " "
                                               (:taxonomy-family data)))))))))

(defn update-view
  [data owner {:keys [taxonomy-id]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :data nil))
    om/IDidMount
    (did-mount [_]
      (rest/get-x (str "/taxonomy/" taxonomy-id)
                  #(om/update! data :data (:body %))))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! data :data nil))
    om/IRender
    (render [_]
      (if (nil? (:data data))
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))
        (om/build update/update-component data)))))

(defn manage-view
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :species nil)
      (om/update! data :known-species nil))
    om/IDidMount
    (did-mount [_]
      (rest/get-x (str "/taxonomy/survey/" (state/get-survey-id))
                  (fn [resp]
                    (om/update! data :species (into #{} (:body resp)))
                    (om/update! data :species-search {})))
      (rest/get-x "/taxonomy"
                  (fn [resp]
                    (om/update! data :known-species
                                (into {}
                                      (map (fn [x]
                                             (hash-map (get x :taxonomy-id) x))
                                           (:body resp)))))))
    om/IRender
    (render [_]
      (if (and (get data :species-search) (get data :known-species))
        (om/build manage/manage-component data)
        (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"}))))))

(defn species-menu-component
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
      (rest/get-resource (str "/taxonomy/survey/"
                              (get-in (state/app-state-cursor)
                                      [:selected-survey :survey-id :value]))
                         #(om/update! data :list (:body %)))
      (let [ch (om/get-state owner :chan)]
        (go
          (loop []
            (let [r (<! ch)]
              (cond
                (= (:event r) :remove)
                (om/transact! data :list #(remove (fn [x] (= x (:data r))) %))))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! data :list nil))
    om/IRenderState
    (render-state [_ state]
      (if (:list data)
        (dom/div #js {:className "section"}
                 (dom/div #js {:className "simple-menu scroll"}
                          (if (empty? (:list data))
                            (om/build util/blank-slate-beta-component {}
                                      {:opts {:item-name (tr/translate ::item-name)}})
                            (om/build-all species-list-component
                                          (sort-by :taxonomy-label (:list data))
                                          {:key :taxonomy-id
                                           :init-state state})))
                 (dom/div #js {:className "sep"})
                 (dom/button #js {:className "btn btn-primary"
                                  :onClick #(do (nav/nav!
                                                 (str "/"
                                                      (get-in (state/app-state-cursor)
                                                              [:selected-survey :survey-id :value])
                                                      "/taxonomy"))
                                                (nav/analytics-event "survey-species" "create-click"))}
                             (dom/span nil)
                             " " (tr/translate ::manage-species))
                 (dom/button #js {:className "btn btn-default"
                                  :onClick #(do (nav/nav! "/taxonomy")
                                                (nav/analytics-event "survey-species" "advanced-click"))}
                             (tr/translate :words/advanced)))
        (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"}))))))
