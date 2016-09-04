(ns camelot.component.species.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [camelot.component.species.update :as update]
            [camelot.component.species.manage :as manage]
            [camelot.component.util :as util]
            [camelot.translation.core :as tr]))

(defn species-list-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed"
                    :onClick #(nav/nav! (str "/taxonomy/" (:taxonomy-id data)))}
               (dom/span #js {:className "menu-item-title"}
                         (:taxonomy-label data))
               (dom/span #js {:className "menu-item-description"}
                         (when (:taxonomy-common-name data)
                           (dom/div nil
                                    (tr/translate :taxonomy/taxonomy-common-name.label) ": "
                                    (:taxonomy-common-name data)))
                         (when (:taxonomy-class data)
                           (str (tr/translate :taxonomy/taxonomy-class.label)
                                ": " (:taxonomy-class data) "; "))
                         (when (:taxonomy-order data)
                           (str (tr/translate :taxonomy/taxonomy-order.label) ": "
                                (:taxonomy-order data) "; "))
                         (when (:taxonomy-family data)
                           (str (tr/translate :taxonomy/taxonomy-family.label) ": "
                                (:taxonomy-family data))))))))

(defn update-view
  [data owner {:keys [taxonomy-id]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :data nil)
      (rest/get-x (str "/taxonomy/" taxonomy-id)
                  #(om/update! data :data (:body %))))
    om/IRender
    (render [_]
      (when-not (nil? (:data data))
        (om/build update/update-component data)))))

(defn manage-view
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
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
      (when (and (get data :species-search) (get data :known-species))
        (om/build manage/manage-component data)))))

(defn species-menu-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource (str "/taxonomy/survey/"
                              (get-in (state/app-state-cursor)
                                      [:selected-survey :survey-id :value]))
                         #(om/update! data :list (:body %))))
    om/IRender
    (render [_]
      (when (:list data)
        (dom/div #js {:className "section"}
                 (dom/div #js {:className "simple-menu scroll"}
                          (if (empty? (:list data))
                            (om/build util/blank-slate-beta-component {}
                                      {:opts {:item-name (tr/translate ::item-name)}})
                            (om/build-all species-list-component
                                          (sort-by :taxonomy-label (:list data))
                                          {:key :taxonomy-id})))
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
                             (tr/translate :words/advanced)))))))
