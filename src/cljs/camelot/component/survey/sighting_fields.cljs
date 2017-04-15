(ns camelot.component.survey.sighting-fields
  "Management of sighting fields."
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.translation.core :as tr]
            [camelot.state :as state]
            [cljs.core.async :refer [<! chan >!]]
            [goog.date :as date]
            [camelot.util.data :as data])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn sighting-field-menu-item
  "Menu item representing a single sighting field."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item"
                    :onClick #(om/update! data ::selected-field-id (:sighting-field-id data))}
               (:sighting-field-label data)))))

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
                                           (sort-by :sighting-field-name)))
                        (dom/div nil
                         (dom/label nil (tr/translate :words/add)))
                        (dom/div nil
                         (dom/button #js {:className "btn btn-primary"
                                          :onClick #()}
                                     (tr/translate ::new-field))))))))

(defn field-translation
  [field suffix]
  (let [ns "camelot.component.survey.sighting-fields"]
    (tr/translate (keyword (str ns "/" (name field) suffix)))))

(defn text-input-component
  [data owner {:keys [field attrs]}]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "field-label"}
                          (tr/translate (field-translation field ".label")))
               (dom/input (clj->js (merge {:className "field-input"
                                           :onChange #(om/update! data [:buffer field]
                                                                  (.. % -target -value))
                                           :value (get-in data [:buffer field :value])
                                           :title (tr/translate (field-translation field ".description"))}
                                          (or attrs {}))))))))

(defn update-button-component
  "Submit an update request."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "btn btn-primary"}
                  (tr/translate :words/update)))))

(defn revert-button-component
  "Revert any changes."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "btn btn-default"}
                  (tr/translate :words/revert)))))

(defn select-option-component
  [[key value] owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value key} value))))

(defn select-component
  [data owner {:keys [field attrs options]}]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "field-label"}
                          (tr/translate (field-translation field ".label")))
               (dom/select (clj->js (merge {:className "field-input"
                                            :onChange #(om/update! data [:buffer field]
                                                                   (.. % -target -value))
                                            :value (get-in data [:buffer field :value])
                                            :title (tr/translate (field-translation field ".description"))}
                                           (or attrs {})))
                           (om/build-all select-option-component options))))))

(defn edit-component
  "Component for editing sighting field details."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (om/build text-input-component data {:opts {:field :sighting-field-label}})
               (om/build text-input-component data {:opts {:field :sighting-field-key}})
               (om/build select-component data {:opts {:field :sighting-field-datatype
                                                       :options {"text" (tr/translate :datatype/text)
                                                                 "textarea" (tr/translate :datatype/textarea)
                                                                 "number" (tr/translate :datatype/number)
                                                                 "select" (tr/translate :datatype/select)}}})
               (om/build select-component data {:opts {:field :sighting-field-required
                                                       :options {"true" (tr/translate :words/yes)
                                                                 "false" (tr/translate :words/no)}}})
               (om/build text-input-component data {:opts {:field :sighting-field-default}})
               (om/build select-component data {:opts {:field :sighting-field-affects-independence
                                                       :options {"true" (tr/translate :words/yes)
                                                                 "false" (tr/translate :words/no)}}})
               (om/build text-input-component data {:opts {:field :sighting-field-ordering
                                                           :attrs {:type "number"}}})
               (dom/div #js {:className "button-container"}
                        (om/build revert-button-component data)
                        (om/build update-button-component data))))))

(defn manage-fields-component
  "Top-level component for managing a survey's sighting fields."
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-x "/sighting-fields"
                  (fn [r] (om/update! data ::sighting-fields
                                      (data/key-by :sighting-field-id (:body r)))))
      (rest/get-x "/surveys"
                  (fn [r] (om/update! data ::surveys (:body r))))
      (let [chan (om/get-state owner ::chan)]
        (go
          (loop []
            (let [r (<! chan)]
              (condp (:event r) =
                :save
                (om/transact! data ::sighting-fields #(assoc % (:sighting-field-id r) r)))
              (recur))))))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! data ::sighting-fields nil)
      (om/update! data ::selected-field-id nil)
      (om/update! data ::surveys nil)
      (om/update! data ::buffer nil))
    om/IRender
    (render [_]
      (if (and (::surveys data) (::sighting-fields data))
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (tr/translate ::page-title)))
                 (dom/div #js {:className "section-container"}
                          (om/build menu-component data))
                 (dom/div #js {:className "section-container"}
                          (om/build edit-component data)))
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))))))
