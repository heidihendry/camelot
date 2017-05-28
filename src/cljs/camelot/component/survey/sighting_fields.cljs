(ns camelot.component.survey.sighting-fields
  "Management of sighting fields."
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.translation.core :as tr]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [cljs.core.async :refer [<! chan >!]]
            [goog.date :as date]
            [camelot.validation.validated-component :as vc]
            [camelot.util.data :as data])
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [camelot.macros.ui.validation :refer [with-validation]]))

(def sighting-field-input-keys
  "Minimal set of keys which a sighting field must possess."
  #{:sighting-field-key
    :sighting-field-label
    :sighting-field-datatype
    :sighting-field-default
    :sighting-field-required
    :sighting-field-affects-independence
    :sighting-field-ordering})

(defn- update-buffer
  "Set buffer based the selected sighting-field ID."
  [data sf-id]
  (let [vs (data/require-keys (get-in data [::sighting-fields sf-id])
                              sighting-field-input-keys)]
    (om/update! data ::buffer vs)))

(defn- select-sighting-field
  "Select the sighting field with the given ID."
  [data sf-id]
  (om/update! data ::selected-sighting-field-id sf-id)
  (update-buffer data sf-id))

(defn sighting-field-menu-item
  "Menu item representing a single sighting field."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item"
                    :onClick #(let [sid (get-in data [::item :sighting-field-id])]
                                (nav/analytics-event "sighting-fields" "survey-field-click")
                                (select-sighting-field (::context data) sid))}
               (get-in data [::item :sighting-field-label])))))

(defn menu-component
  "Side menu for sighting fields within a survey."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (dom/div nil
                                 (dom/label nil (tr/translate :words/edit)))
                        (om/build-all sighting-field-menu-item
                                      (->> (::sighting-fields data)
                                           vals
                                           (filter #(= (:survey-id %) (state/get-survey-id)))
                                           (sort-by :sighting-field-label)
                                           (map #(hash-map ::context data ::item %))))
                        (dom/div nil
                         (dom/label nil (tr/translate :words/add)))
                        (dom/div nil
                         (dom/button #js {:className "btn btn-primary"
                                          :onClick #(do
                                                      (nav/analytics-event "sighting-fields" "new-field-click")
                                                      (select-sighting-field data nil))}
                                     (tr/translate ::new-field))))))))

(defn field-translation
  "Return the translation for the field. Suffix may be either '.label' or '.description'."
  [field suffix]
  (let [ns "camelot.component.survey.sighting-fields"]
    (tr/translate (keyword (str ns "/" (name field) suffix)))))

(defn update-button-component
  "Submit an update request."
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/button #js {:className "btn btn-primary"
                       :disabled (if (::validated data) "" "disabled")
                       :onClick #(do
                                   (nav/analytics-event "sighting-fields" "update-click")
                                   (go (>! (::chan state) {:event :update
                                                           :buffer data})))}
                  (tr/translate :words/update)))))

(defn revert-button-component
  "Revert any changes."
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/button #js {:className "btn btn-default"
                       :onClick #(do
                                   (nav/analytics-event "sighting-fields" "revert-click")
                                   (go (>! (::chan state) {:event :revert})))}
                  (tr/translate :words/revert)))))

(defn select-option-component
  "Render a component for a select option."
  [[key value] owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value key} value))))

(defn select-component
  "Render a component for selections, adding in any additional attributes specified by `attrs`.
Options for select are given by the `options` option."
  [data owner {:keys [field attrs options]}]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "field-label"}
                          (tr/translate (field-translation field ".label")))
               (dom/select (clj->js (merge {:className "field-input"
                                            :onChange #(om/update! data field
                                                                   (.. % -target -value))
                                            :value (get data field)
                                            :title (tr/translate (field-translation field ".description"))}
                                           (or attrs {})))
                           (om/build-all select-option-component options))))))

(defn text-input-component
  "Render a component for text input, adding in any additional attributes specified by `attrs`."
  [data owner {:keys [field attrs]}]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "field-label"}
                          (tr/translate (field-translation field ".label")))
               (dom/input (clj->js (merge {:className "field-input"
                                           :onChange #(om/update! data field
                                                                  (.. % -target -value))
                                           :value (get data field)
                                           :title (tr/translate (field-translation field ".description"))}
                                          (or attrs {}))))))))

(defn edit-component
  "Component for editing sighting field details."
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:result-vchan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [rvchan (om/get-state owner :result-vchan)]
        (om/set-state! owner :validation-chan
                       (vc/component-validator rvchan))
        (go-loop []
          (let [{:keys [validated]} (<! rvchan)]
            (om/update! data ::validated validated)
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (go
        (>! (om/get-state owner :result-vchan) {:command :unmount})))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "section"}
               (with-validation (:validation-chan state) dom/div {}
                 (om/build text-input-component data
                           {:data-key :sighting-field-label
                            :validators [(vc/required) (vc/max-length 255)]
                            :params {:opts {:field :sighting-field-label}}})
                 (om/build text-input-component data
                           {:data-key :sighting-field-key
                            :validators []
                            :params {:opts {:field :sighting-field-key}}})
                 (om/build select-component data
                           {:data-key :sighting-field-datatype
                            :validators []
                            :params {:opts
                                     {:field :sighting-field-datatype
                                      :options {"text" (tr/translate :datatype/text)
                                                "textarea" (tr/translate :datatype/textarea)
                                                "number" (tr/translate :datatype/number)
                                                "select" (tr/translate :datatype/select)}}}})
                 (om/build select-component data
                           {:data-key :sighting-field-required
                            :validators []
                            :params {:opts
                                     {:field :sighting-field-required
                                      :options {"true" (tr/translate :words/yes)
                                                "false" (tr/translate :words/no)}}}})
                 (om/build text-input-component data
                           {:data-key :sighting-field-default
                            :validators []
                            :params {:opts {:field :sighting-field-default}}})
                 (om/build select-component data
                           {:data-key :sighting-field-affects-independence
                            :validators []
                            :params
                            {:opts {:field :sighting-field-affects-independence
                                    :options {"true" (tr/translate :words/yes)
                                              "false" (tr/translate :words/no)}}}})
                 (om/build text-input-component data
                           {:data-key :sighting-field-ordering
                            :validators []
                            :params
                            {:opts {:field :sighting-field-ordering
                                    :attrs {:type "number"}}}}))

               (dom/div #js {:className "button-container"}
                        (om/build revert-button-component data {:state state})
                        (om/build update-button-component data {:state state}))))))

(defn manage-fields-component
  "Top-level component for managing a survey's sighting fields."
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {::chan (chan)})
    om/IWillMount
    (will-mount [_]
      (rest/get-x "/sighting-fields"
                  (fn [r] (om/update! data ::sighting-fields
                                      (data/key-by :sighting-field-id (:body r)))))
      (rest/get-x "/surveys"
                  (fn [r] (om/update! data ::surveys (:body r))))
      (update-buffer data nil)
      (let [ch (om/get-state owner ::chan)]
        (go-loop []
          (let [r (<! ch)]
            (condp = (:event r)
              :update
              (let [v (assoc (:buffer r) :survey-id (state/get-survey-id))]
                ;; TODO post to server and transact! result
                (om/transact! data ::sighting-fields #(assoc % (:sighting-field-id r) v)))

              :revert
              (do
                (select-sighting-field data (::selected-sighting-field-id @data))))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! data ::sighting-fields nil)
      (om/update! data ::selected-sighting-field-id nil)
      (om/update! data ::surveys nil)
      (om/update! data ::buffer nil))
    om/IRenderState
    (render-state [_ state]
      (if (and (::surveys data) (::sighting-fields data))
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (tr/translate ::page-title)))
                 (dom/div #js {:className "section-container"}
                          (om/build menu-component data))
                 (dom/div #js {:className "section-container"}
                          (om/build edit-component (::buffer data) {:state state})))
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))))))
