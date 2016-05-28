(ns camelot.component.import-dialog
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [clojure.string :as str]
            [camelot.rest :as rest]))

(defn generic-select
  [field e]
  (when-not (:selections (state/import-dialog-state))
    (om/update! (state/import-dialog-state) :selections {}))
  (om/update! (:selections (state/import-dialog-state)) field (.. e -target -value))
  (rest/post-import-state {:data (deref (:selections (state/import-dialog-state)))}
                          #(om/update! (state/import-dialog-state)
                                       :options (:body %))))

(defn survey-select
  [e]
  (generic-select :survey e))

(defn survey-site-select
  [e]
  (generic-select :survey-site e))

(defn trap-station-select
  [e]
  (generic-select :trap-station e))

(defn trap-station-session-select
  [e]
  (generic-select :trap-station-session e))

(defn trap-station-session-camera-select
  [e]
  (generic-select :trap-station-session-camera e))

(defn option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:vkey data)} (:desc data)))))

(defn location-selector-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (prn (get-in (state/resources-state) [:settings :root-path :value]))
      (dom/div #js {:className "import-location-selector"}
               (dom/div nil
                        (dom/label nil "Import from")
                        (dom/div #js {:className "import-folder"}
                                 (subs
                                  (get (state/import-dialog-state) :path)
                                  (count (get-in (state/resources-state) [:settings :root-path :value])))))
               (dom/div nil
                        (dom/label nil "Survey")
                        (dom/select #js {:className "field-input" :onChange survey-select}
                                    (om/build-all option-component
                                                  (cons {:vkey nil :desc "Select..."}
                                                        (get-in (state/import-dialog-state)
                                                                [:options :surveys])) {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Survey Site")
                        (dom/select #js {:className "field-input" :onChange survey-site-select}
                                    (om/build-all option-component
                                                  (cons {:vkey nil :desc "Select..."}
                                                        (get-in (state/import-dialog-state)
                                                                [:options :survey-sites])) {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Trap Station")
                        (dom/select #js {:className "field-input" :onChange trap-station-select}
                                    (om/build-all option-component
                                                  (cons {:vkey nil :desc "Select..."}
                                                        (get-in (state/import-dialog-state)
                                                                [:options :trap-stations])) {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Trap Station Session")
                        (dom/select #js {:className "field-input" :onChange trap-station-session-select}
                                    (om/build-all option-component
                                                  (cons {:vkey nil :desc "Select..."}
                                                        (get-in (state/import-dialog-state)
                                                                [:options :trap-station-sessions])) {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Session Camera")
                        (dom/select #js {:className "field-input" :onChange trap-station-session-camera-select}
                                    (om/build-all option-component
                                                  (cons {:vkey nil :desc "Select..."}
                                                        (get-in (state/import-dialog-state)
                                                                [:options :trap-station-session-cameras])) {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Notes")
                        (dom/textarea #js {:className "field-input" :cols "42" :rows "2"}))))))

(defn import-dialog-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (if (get-in app [:import-dialog :visible])
        (do
          (when-not (:options (state/import-dialog-state))
            (rest/post-import-state {:data (deref (:selections (state/import-dialog-state)))}
                                    #(om/update! (state/import-dialog-state)
                                                 :options (:body %))))
          (dom/div #js {:className "content"}
                   (dom/h3 nil "Import Media")
                   (om/build location-selector-component (:import-dialog app))
                   (dom/div #js {:className "button-container"}
                            (dom/button #js {:className "btn btn-primary"
                                             :onClick #(om/update! (state/import-dialog-state) :visible false)}
                                        "Import")
                            (dom/button #js {:className "btn btn-default"
                                             :onClick #(om/update! (state/import-dialog-state) :visible false)}
                                        "Cancel"))))
        (dom/span nil "")))))

(defn not-found-page-component
  "Page not found"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h2 nil "Page Not Found")))))
