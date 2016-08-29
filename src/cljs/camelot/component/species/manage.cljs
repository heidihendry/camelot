(ns camelot.component.species.manage
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [camelot.nav :as nav]
            [camelot.state :as state]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.component.species-search :as search]
            [om.dom :as dom])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn remove-species
  [taxonomy-id cb]
  (let [sid (get-in (state/app-state-cursor)
                    [:selected-survey :survey-id :value])]
    (rest/delete-x (str "/taxonomy/" taxonomy-id "/survey/" sid) cb)))

(defn species-row-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/tr #js {:onClick #(go (>! (:rm-chan state) data))}
              (dom/td nil (:taxonomy-genus data))
              (dom/td nil (:taxonomy-species data))
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
              (remove-species (:taxonomy-id r)
                              (fn [x]
                                (do
                                  (om/transact! data :species #(into #{} (disj % r)))
                                  (nav/analytics-event "species-manage" "species-remove-click"))))
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
                                                     (sort-by :taxonomy-genus (sort-by :taxonomy-species
                                                                                       (:species data)))
                                                     {:init-state state}))))
        (dom/div #js {:className "no-species-found"}
                 (dom/p nil
                        "Search and add species using the options to the right"))))))

(defn expected-species-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section survey-details-pane"}
               (dom/label #js {:className "field-label"} "Expected Species")
               (om/build survey-species-list data)
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick #(do (nav/analytics-event "species-manage" "done-click")
                                                       (nav/nav-up!))}
                                    "Done"))))))

(defn species-search-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (when (:species-search data)
        (dom/div #js {:className "section"}
                 (om/build search/species-search-component (:species-search data)))))))

(defn manage-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (when (get app :data)
        (if-let [sel (get-in app [:data :species-search :selection])]
          (do
            (rest/post-x "/species/create"
                         {:data {:survey-id (get-in (state/app-state-cursor)
                                                    [:selected-survey :survey-id :value])
                                 :species [(select-keys sel [:species :genus :id])]}}
                         (fn [x]
                           (do
                             (prn (first (:body x)))
                             (om/transact! (:data app) :species #(conj % {:taxonomy-id (:taxonomy-id (first (:body x)))
                                                                   :taxonomy-species (:species sel)
                                                                   :taxonomy-genus (:genus sel)}))
                             (om/update! (:species-search (:data app)) :selection nil))))))
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil "Manage Species"))
                 (dom/div nil
                          (dom/div #js {:className "section-container"}
                                   (om/build expected-species-component (:data app)))
                          (dom/div #js {:className "section-container"}
                                   (om/build species-search-component (:data app)))))))))
