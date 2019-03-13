(ns camelot.component.survey.settings
  (:require [camelot.state :as state]
            [om.dom :as dom]
            [om.core :as om]
            [camelot.translation.core :as tr]
            [camelot.nav :as nav]
            [camelot.rest :as rest]))

(defn navigate-away
  [data]
  (nav/nav-up!))

(defn blank?
  [d]
  (or (nil? d) (empty? (.trim d))))

(defn delete
  "Delete the survey and trigger a removal event."
  [event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (js/confirm (tr/translate ::confirm-delete))
    (rest/delete-x (str "/surveys/" (state/get-survey-id))
                   #(nav/nav! "/organisation"))))

(defn validate-form
  [data]
  (cond->> {}
    (blank? (get-in data [::data :survey-name :value]))
    (merge {:survey-name (tr/translate ::validation-survey-name-blank)})

    (contains? (::other-survey-names data) (get-in data [::data :survey-name :value]))
    (merge {:survey-name (tr/translate ::validation-survey-name-duplicate)})

    (blank? (get-in data [::data :survey-notes :value]))
    (merge {:survey-notes (tr/translate ::validation-survey-notes-blank)})

    (not (re-find #"^[0-9]+$" (or (get-in data [::data :survey-sighting-independence-threshold :value]) "")))
    (merge {:survey-sighting-independence-threshold (tr/translate ::validation-survey-sighting-independence-threshold-not-a-number)})))

(defn update-handler
  [data]
  (nav/analytics-event "taxonomy-update" "submit")
  (rest/put-x (str "/surveys/" (state/get-survey-id)),
              {:data (select-keys (deref data)
                                  [:survey-name :survey-notes :survey-sighting-independence-threshold])}
              navigate-away))

(defn submit-button
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [validation-passes (empty? (:validation-errors data))]
        (dom/button #js {:className "btn btn-primary"
                         :disabled (if validation-passes
                                       "" "disabled")
                         :title (when-not validation-passes
                                  (tr/translate ::validation-error-title))
                         :onClick (partial update-handler data)}
                    (tr/translate :words/update))))))

(defn cancel-button
  "Navigate away without saving the current form state"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "btn btn-default"
                       :onClick navigate-away}
                  (tr/translate :words/cancel)))))

(defn- form-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [validation-errors (validate-form data)]
        (dom/div nil
                 (dom/label #js {:className "field-label required"}
                            (tr/translate :survey/survey-name.label))
                 (let [field :survey-name]
                   (dom/input #js {:className "field-input"
                                   :title (tr/translate :survey/survey-name.description)
                                   :onChange #(om/update! data [::data field :value]
                                                          (.. % -target -value))
                                   :value (get-in data [::data field :value])}))
                 (dom/div #js {:className "validation-warning"
                               :style #js {:position "relative" :top "-0.8rem"}}
                          (:survey-name validation-errors))
                 (dom/label #js {:className "field-label"}
                            (tr/translate :survey/survey-notes.label))
                 (let [field :survey-notes]
                   (dom/textarea #js {:className "field-input"
                                      :title (tr/translate :survey/survey-notes.description)
                                      :onChange #(om/update! data [::data field :value]
                                                             (.. % -target -value))
                                      :value (get-in data [::data field :value])}))
                 (dom/div #js {:className "validation-warning"
                               :style #js {:position "relative" :top "-0.8rem"}}
                          (:survey-notes validation-errors))
                 (dom/label #js {:className "field-label"}
                            (tr/translate :survey/survey-sighting-independence-threshold.label))
                 (let [field :survey-sighting-independence-threshold]
                   (dom/input #js {:className "field-input"
                                   :type "number"
                                   :title (tr/translate :survey/survey-sighting-independence-threshold.description)
                                   :onChange #(om/update! data [::data field :value]
                                                          (.. % -target -value))
                                   :value (get-in data [::data field :value])}))
                 (dom/div #js {:className "validation-warning"
                               :style #js {:position "relative" :top "-0.8rem"}}
                          (:survey-sighting-independence-threshold validation-errors))
                 (dom/div #js {:className "button-container"}
                          (om/build cancel-button (::data data))
                          (om/build submit-button (assoc (::data data)
                                                         :validation-errors validation-errors))))))))

(defn edit-details-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-x (str "/surveys/" (state/get-survey-id))
                  #(om/update! data ::data (:body %)))
      (rest/get-x "/surveys"
                  (fn [r] (om/update! data ::other-survey-names
                                      (->> (:body r)
                                           (filter #(not= (:survey-id %)
                                                          (state/get-survey-id)))
                                           (map :survey-name)
                                           (into #{}))))))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! data ::data nil)
      (om/update! data ::other-survey-names nil))
    om/IRender
    (render [_]
      (if (::data data)
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (let [v (get-in data [::data :survey-name :value])]

                                        (if (empty? v)
                                          (tr/translate ::survey-name-placeholder)
                                          v))))
                 (dom/div #js {:className "single-section"}
                          (om/build form-component data)))
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))))))

(defn menu-item-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      )))

(defn settings-menu-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu scroll"}
                        (dom/div #js {:className "menu-item"
                                      :onClick #(nav/nav! (str "/" (state/get-survey-id) "/details"))}
                                 (dom/span #js {:className "menu-item-title"}
                                           (tr/translate ::details)))
                        (dom/div #js {:className "menu-item"
                                      :onClick #(nav/nav! (str "/" (state/get-survey-id) "/sighting-fields"))}
                                 (dom/span #js {:className "menu-item-title"}
                                           (tr/translate ::sighting-fields))))
               (dom/div #js {:className "sep"})
               (dom/button #js {:className "btn btn-dangerous"
                                :onClick delete}
                           (tr/translate ::delete-survey))))))
