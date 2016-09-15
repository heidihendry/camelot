(ns camelot.component.import-dialog
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.component.albums :as albums]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [clojure.string :as str]
            [camelot.rest :as rest]
            [camelot.translation.core :as tr]))

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
  (om/update! (state/app-state-cursor) :loading (tr/translate :words/uploading))
  (nav/analytics-event "upload" "import-begin")
  (rest/post-x "/import/media"
               {:data (deref (:selections (state/import-dialog-state)))}
               #(do (om/update! (state/app-state-cursor) :loading nil)
                    (nav/analytics-event "upload" "import-success")
                    (albums/reload-albums))
               #(do (om/update! (state/app-state-cursor) :loading nil)
                    (nav/analytics-event "upload" "import-failure")
                    (rest/set-error-state! "/import/media" %))))

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

(defn get-root-path
  []
  (get-in (state/app-state-cursor)
          [:view :settings :selected-resource :details :root-path :value]))

(defn simple-import-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (when-not (get-in (state/import-dialog-state) [:selections :folder])
        (om/update! (get (state/import-dialog-state) :selections)
                    :folder (subs (get (state/import-dialog-state) :path)
                                  (count (get-root-path)))))
      (dom/div #js {:className "import-location-selector"}
               (dom/div nil
                        (dom/label nil (tr/translate ::import-from))
                        (dom/div #js {:className "import-folder"}
                                 (get-in (state/import-dialog-state) [:selections :folder])))
               (dom/div nil
                        (dom/label nil (tr/translate :words/notes))
                        (dom/textarea #js {:className "field-input" :cols "42" :rows "4"
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
                   (dom/h3 nil (tr/translate ::import-media))
                   (om/build simple-import-component (:import-dialog app))
                   (dom/div #js {:className "button-container"}
                            (dom/button #js {:className "btn btn-primary"
                                             :onClick #(do
                                                         (post-import)
                                                         (om/update! (state/import-dialog-state) :selections {})
                                                         (om/update! (state/import-dialog-state) :visible false)
                                                         (nav/analytics-event "import-dialog" "import"))}
                                        (tr/translate :words/import))
                            (dom/button #js {:className "btn btn-default"
                                             :onClick #(do
                                                         (om/update! (state/import-dialog-state) :selections {})
                                                         (om/update! (state/import-dialog-state) :visible false)
                                                         (nav/analytics-event "import-dialog" "cancel"))}
                                        (tr/translate :words/cancel)))))
        (dom/span nil "")))))
