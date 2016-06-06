(ns camelot.component.import-dialog
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.component.albums :as albums]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [clojure.string :as str]
            [camelot.rest :as rest]))

(def select-priorities
  {:survey 0
   :survey-site 1
   :trap-station 2
   :trap-station-session 3
   :trap-station-session-camera 4})

(defn- invalidate-settings-following
  [field]
  (let [p (get select-priorities field)]
    (doseq [[k v] select-priorities]
      (when (> v p)
        (om/update! (:selections (state/import-dialog-state)) k nil)))))

(defn post-import-state
  []
  (rest/post-x "/import/options" {:data (deref (:selections (state/import-dialog-state)))}
               #(om/update! (state/import-dialog-state)
                            :options (:body %))))

(defn post-import
  []
  (om/update! (state/app-state-cursor) :loading "Uploading")
  (cnav/analytics-event "upload" "import-begin")
  (rest/post-x "/import/media"
               {:data (deref (:selections (state/import-dialog-state)))}
               #(do (om/update! (state/app-state-cursor) :loading nil)
                    (cnav/analytics-event "upload" "import-success")
                    (albums/reload-albums))
               #(do (om/update! (state/app-state-cursor) :loading nil)
                    (cnav/analytics-event "upload" "import-failure"))))

(defn- generic-select
  [field e]
  (invalidate-settings-following field)
  (om/update! (:selections (state/import-dialog-state)) field (.. e -target -value))
  (post-import-state))

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

(defn simple-import-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (when-not (get-in (state/import-dialog-state) [:selections :folder])
        (om/update! (get (state/import-dialog-state) :selections)
                    :folder (subs (get (state/import-dialog-state) :path)
                                  (count (get-in (state/resources-state) [:settings :root-path :value])))))
      (dom/div #js {:className "import-location-selector"}
               (dom/div nil
                        (dom/label nil "Import from")
                        (dom/div #js {:className "import-folder"}
                                 (get-in (state/import-dialog-state) [:selections :folder])))
               (dom/div nil
                        (dom/label nil "Notes")
                        (dom/textarea #js {:className "field-input" :cols "42" :rows "4"
                                           :onChange #(om/transact! (:selections (state/import-dialog-state))
                                                                    :notes
                                                                    (fn [_] (.. % -target -value)))}))))))

(defn location-selector-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (when-not (get-in (state/import-dialog-state) [:selections :folder])
        (om/update! (get (state/import-dialog-state) :selections)
                    :folder (subs (get (state/import-dialog-state) :path)
                                  (count (get-in (state/resources-state) [:settings :root-path :value])))))
      (dom/div #js {:className "import-location-selector"}
               (dom/div nil
                        (dom/label nil "Import from")
                        (dom/div #js {:className "import-folder"}
                                 (get-in (state/import-dialog-state) [:selections :folder])))
               (dom/div nil
                        (dom/label nil "Survey")
                        (dom/select #js {:className "field-input" :onChange survey-select}
                                    (om/build-all option-component
                                                  (cons {:vkey nil :desc "Select..."}
                                                        (get-in (state/import-dialog-state)
                                                                [:options :surveys])) {:key :vkey})))
               (dom/div nil
                        (dom/label nil "Survey Site")
                        (dom/select #js {:className "field-input"
                                         :onChange survey-site-select}
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
                        (dom/textarea #js {:className "field-input" :cols "42" :rows "2"
                                           :onChange #(om/transact! (:selections (state/import-dialog-state))
                                                                    :notes
                                                                    (fn [_] (.. % -target -value)))}))))))

(defn import-dialog-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (if (get-in app [:import-dialog :visible])
        (do
          (when-not (:selections (state/import-dialog-state))
            (om/update! (state/import-dialog-state) :selections {}))
          (when-not (:options (state/import-dialog-state))
            (post-import-state))
          (dom/div #js {:className "content"}
                   (dom/h3 nil "Import Media")
                   (om/build simple-import-component (:import-dialog app))
                   (dom/div #js {:className "button-container"}
                            (dom/button #js {:className "btn btn-primary"
                                             :onClick #(do
                                                         (post-import)
                                                         (om/update! (state/import-dialog-state) :selections {})
                                                         (om/update! (state/import-dialog-state) :visible false))}
                                        "Import")
                            (dom/button #js {:className "btn btn-default"
                                             :onClick #(do
                                                         (om/update! (state/import-dialog-state) :selections {})
                                                         (om/update! (state/import-dialog-state) :visible false))}
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
