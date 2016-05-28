(ns camelot.component.import-dialog
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [clojure.string :as str]))

(defn survey-select
  []
  nil)

(defn survey-site-select
  []
  nil)

(defn trap-station-select
  []
  nil)

(defn trap-station-session-select
  []
  nil)

(defn camera-select
  []
  nil)

(defn option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:vkey data)} "Select..."))))

(defn location-selector-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (prn (get-in (state/resources-state) [:settings :root-path :value]))
      (dom/div #js {:className "import-location-selector"}
               (dom/div nil
                        (dom/label nil "Folder")
                        (dom/div nil
                                 (subs
                                  (get (state/import-dialog-state) :path)
                                  (count (get-in (state/resources-state) [:settings :root-path :value])))))
               (dom/div nil
                        (dom/label nil "Survey")
                        (dom/select #js {:className "field-input" :onChange survey-select} (om/build-all option-component [1] {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Survey Site")
                        (dom/select #js {:className "field-input" :onChange survey-site-select} (om/build-all option-component [1] {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Trap Station")
                        (dom/select #js {:className "field-input" :onChange trap-station-select} (om/build-all option-component [1] {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Trap Station Session")
                        (dom/select #js {:className "field-input" :onChange trap-station-session-select} (om/build-all option-component [1] {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Camera")
                        (dom/select #js {:className "field-input" :onChange camera-select} (om/build-all option-component [1] {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Notes")
                        (dom/textarea #js {:className "field-input" :cols "42" :rows "3"}))))))

(defn import-dialog-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (if (get-in app [:import-dialog :visible])
        (dom/div #js {:className "content"}
                 (dom/h3 nil "Import Media")
                 (om/build location-selector-component (:import-dialog app))
                 (dom/div #js {:className "button-container"}
                          (dom/button #js {:className "btn btn-primary"
                                           :onClick #(om/update! (state/import-dialog-state) :visible false)}
                                      "Import")
                          (dom/button #js {:className "btn btn-default"
                                           :onClick #(om/update! (state/import-dialog-state) :visible false)}
                                      "Cancel")))
        (dom/span nil "")))))

(defn not-found-page-component
  "Page not found"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h2 nil "Page Not Found")))))
