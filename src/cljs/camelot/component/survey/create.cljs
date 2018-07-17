(ns camelot.component.survey.create
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [camelot.util.feature :as feature]
            [camelot.component.util :as util]
            [camelot.util.cursorise :as cursorise]
            [camelot.nav :as nav]
            [camelot.state :as state]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.component.species-search :as search]
            [om.dom :as dom]
            [camelot.translation.core :as tr])
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
                                      (tr/translate :words/remove)))))))

(defn survey-data-existed?
  []
  (if (seq (get-in (state/app-state-cursor) [:organisation :survey :list]))
    true
    false))

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
              (nav/analytics-event "org-survey-create" "species-remove-click")
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (if (seq (:species data))
        (dom/div #js {:className "survey-species"}
                 (dom/table nil
                            (dom/thead nil
                                       (dom/tr #js {:className "table-heading"}
                                               (dom/th nil (tr/translate :taxonomy/taxonomy-genus.label))
                                               (dom/th nil (tr/translate :taxonomy/taxonomy-species.label))
                                               (dom/th nil "")))
                            (dom/tbody #js {:className "selectable"}
                                       (om/build-all species-row-component
                                                     (:species data)
                                                     {:init-state state
                                                      :key :id}))))
        (dom/div #js {:className "no-species-found"}
                 (dom/p nil
                        (tr/translate ::search-instructions)))))))

(defn navigate-after-creation
  "Navigate back to the organisation top-level page."
  [data resp]
  (if (survey-data-existed?)
    (nav/nav! "/organisation")
    (.reload js/location)))

(defn create-survey-success-handler
  [data resp]
  (let [survey-id (get-in resp [:body :survey-id :value])]
    (om/transact! (state/app-state-cursor) [:organisation :survey :list] #(conj % (cursorise/decursorise (:body resp))))
    (om/update! data :survey-id survey-id)
    (rest/post-x-opts "/species/create"
                      {:species (deref (:species data))
                       :survey-id survey-id}
                      {:success #(do
                                   (navigate-after-creation data resp)
                                   (nav/analytics-event "org-survey" "create"))
                       :always #(om/update! data :submitting-form false)})))

(defn create-survey
  "Create a new survey."
  [data]
  (om/update! data :submitting-form true)
  (let [ps (select-keys data [:survey-name
                              :survey-notes])]
    (rest/post-x-opts "/surveys" ps
                      {:success (partial create-survey-success-handler data)
                       :failure #(om/update! data :submitting-form false)})))

(defn survey-details-completed?
  [data]
  (and (get data :survey-name)
       (not= (get data :survey-name) "")
       (get data :survey-notes)
       (not= (get data :survey-notes) "")))

(defn create-survey-details-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section survey-details-pane"}
               (dom/label #js {:className "field-label required"}
                          (tr/translate ::survey-name))
               (dom/input #js {:className "field-input"
                               :type "text"
                               :placeholder (tr/translate ::survey-name-placeholder)
                               :value (or (get data :survey-name) "")
                               :onChange #(om/update! data
                                                      :survey-name (.. % -target -value))})
               (dom/label #js {:className "field-label required"}
                          (tr/translate ::survey-description))
               (dom/textarea #js {:className "field-input"
                                  :rows "3"
                                  :value (or (get data :survey-notes) "")
                                  :onChange #(om/update! data :survey-notes (.. % -target -value))})
               (dom/label #js {:className "field-label"}
                          (tr/translate ::expected-species))
               (om/build survey-species-list data)
               (dom/div #js {:className "button-container"}
                        (when (survey-data-existed?)
                          (dom/button #js {:className "btn btn-default"
                                           :onClick #(do
                                                       (nav/nav! "/organisation")
                                                       (nav/analytics-event "org-survey-create" "cancel-click"))}
                                      (tr/translate :words/cancel)))
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick #(do
                                                     (nav/analytics-event "org-survey-create" "submit-next-click")
                                                     (create-survey data))
                                         :disabled (if (and (survey-details-completed? data)
                                                            (not (:submitting-form data)))
                                                     "" "disabled")
                                         :title (if (survey-details-completed? data)
                                                  (tr/translate ::submit-title)
                                                  (tr/translate ::validation-error-title))}
                                    (tr/translate ::create-survey)
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
                                      :survey-name nil
                                      :survey-notes ""}))
    om/IRender
    (render [_]
      (if-let [data (get app :create-survey)]
        (do
          (when (get-in data [:species-search :selection])
            (om/transact! data :species
                          #(conj % (deref (get-in data [:species-search :selection]))))
            (om/update! (:species-search data) :selection nil))
          (dom/div #js {:className "split-menu"}
                   (dom/div #js {:className "intro"}
                            (dom/h4 nil (tr/translate ::intro)))
                   (dom/div nil
                            (dom/div #js {:className "section-container"}
                                     (om/build create-survey-details-component data))
                            (dom/div #js {:className "section-container"}
                                     (om/build species-listing-component data)))))
        (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"}))))))
