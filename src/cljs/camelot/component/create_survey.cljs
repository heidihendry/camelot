(ns camelot.component.create-survey
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [camelot.nav :as nav]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.component.species-search :as search]
            [om.dom :as dom])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn species-row-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/tr #js {:onClick #(go (>! (:rm-chan state) data))}
              (dom/td nil (:genus data))
              (dom/td nil (:species data))
              (dom/td nil (dom/button #js {:className "btn btn-default"}
                                      "Remove"))))))

(defn survey-species-list
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:rm-chan (chan)})
    om/IWillUpdate
    (will-update [_ _ _]
      (let [chan (om/get-state owner :rm-chan)]
        (go
          (loop []
            (let [r (<! chan)]
              (om/transact! data :species #(into #{} (disj % r)))
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (if (seq (:species data))
        (dom/div #js {:className "survey-species"}
                 (dom/table nil
                            (dom/thead nil
                                       (dom/tr #js {:className "table-heading"}
                                               (dom/th nil "Genus")
                                               (dom/th nil "Species")
                                               (dom/th nil "")))
                            (dom/tbody #js {:className "selectable"}
                                       (om/build-all species-row-component
                                                     (:species data)
                                                     {:init-state state}))))
        (dom/div #js {:className "no-species-found"}
                 (dom/p nil
                        "Add species from the search menu"))))))

(defn create-survey
  [data]
  (let [ps (select-keys data [:survey-name
                              :survey-notes
                              :survey-sighting-independence-threshold])]
    (rest/post-x "/species/create"
                 {:data {:ids (map :id (data :species))}}
                 (fn []
                   (rest/post-x "/surveys" {:data ps}
                                #(nav/nav! "/library"))))))

(defn create-survey-details-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section survey-details-pane"}
               (dom/label #js {:className "field-label"} "Survey Name")
               (dom/input #js {:className "field-input"
                               :type "text"
                               :placeholder "Survey Name..."
                               :value (:survey-name data)
                               :onChange #(om/update! data
                                                      :survey-name (.. % -target -value))})
               (dom/label #js {:className "field-label"} "Survey Description")
               (dom/textarea #js {:className "field-input"
                                  :rows "3"
                                  :value (:survey-notes data)
                                  :onChange #(om/update! data
                                                         :survey-notes (.. % -target -value))})
               (dom/label #js {:className "field-label"} "Sighting Independence Threshold (minutes)")
               (dom/input #js {:className "field-input"
                               :type "number"
                               :value (:survey-sighting-independence-threshold data)
                               :onChange #(om/update! data
                                                      :survey-sighting-independence-threshold (int (.. % -target -value)))})
               (dom/label #js {:className "field-label"} "Expected Species")
               (om/build survey-species-list data)
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick #(create-survey data)}
                                    "Create Survey"
                                    (dom/span #js {:className "btn-right-icon fa fa-chevron-right"})))))))

(defn species-listing-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (om/build search/species-search-component (:species-search data))))))

(defn create-survey-view-component
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :create-survey {:species-search {}
                                      :species #{}
                                      :sighting-independence-threshold 20
                                      :survey-name nil}))
    om/IRender
    (render [_]
      (if-let [data (get app :create-survey)]
        (do
          (prn data)
          (when (get-in data [:species-search :selection])
            (om/transact! data :species
                          #(conj % (deref (get-in data [:species-search :selection]))))
            (om/update! (:species-search data) :selection nil))
          (prn (om/cursor? data))
          (prn (:species data))
          (dom/div #js {:className "create-survey"}
                   (dom/div #js {:className "intro"}
                            (dom/h4 nil "Create Survey"))
                   (dom/div nil
                            (dom/div #js {:className "section-container"}
                                     (om/build create-survey-details-component data))
                            (dom/div #js {:className "section-container"}
                                     (om/build species-listing-component data)))))))))
