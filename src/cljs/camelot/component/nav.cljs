(ns camelot.component.nav
  (:require [camelot.nav :as nav]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.component.progress-bar :as progress-bar]
            [cljs.core.async :refer [<! chan >! timeout]]
            [camelot.rest :as rest]
            [camelot.translation.core :as tr])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def bulk-import-refresh-timeout 3000)

(defn import-progress
  "Get the progress of the bulk import."
  [data]
  (double (/ (reduce + 0 (vals (select-keys (:import-status data) [:complete :failed])))
             (max (reduce + (vals (:import-status data))) 1))))

(defn settings-hide!
  "Hide the settings panel"
  []
  (let [elt (js/document.getElementById "settings")
        navelt (js/document.getElementById "settings-nav")]
    (when elt
      (set! (.-className elt) ""))
    (when navelt
      (set! (.-className navelt) (clojure.string/replace-first
                                  (.-className navelt) #"active" "")))))

(defn settings-show!
  "Show the settings panel"
  []
  (let [elt (js/document.getElementById "settings")
        navelt (js/document.getElementById "settings-nav")]
    (set! (.-className elt) "show")
    (set! (.-className navelt) (str "active " (.-className navelt)))))

(defn toggle-settings!
  "Toggle the settings panel show state"
  []
  (let [navelt (js/document.getElementById "settings-nav")]
    (if (clojure.string/includes? (.-className navelt) "active")
      (settings-hide!)
      (settings-show!))))

(defn bulk-import-details-panel-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [total (reduce + 0 (vals (:import-status data)))
            progress (import-progress data)]
        (dom/div #js {:id "bulk-import-details-panel"
                      :onClick #(do (.preventDefault %)
                                    (.stopPropagation %))
                      :className (if (and (pos? total)
                                          (:show-bulk-import-details data))
                        "expanded"
                        "")}
                 (dom/div #js {:className "bulk-import-details-container"}
                          (dom/span #js {:className "progress-numbers"}
                                    (+ (get-in data [:import-status :complete])
                                       (get-in data [:import-status :failed]))
                                    " " (tr/translate :words/of-lc) " "
                                    total)
                          (dom/label #js {:className "field-label"}
                                     (tr/translate ::bulk-import-progress-label))
                          (dom/div #js {:className "bulk-import-progress-bar"}
                                   (om/build progress-bar/component data
                                             {:state (assoc (:import-status data)
                                                            :total total)})
                                   (when-not (= progress 1)
                                     (dom/span #js {:className "fa fa-times-circle cancel-button"
                                                    :title (tr/translate :words/cancel)})))
                          (if (zero? (get-in data [:import-status :failed]))
                            (when (= progress 1)
                              (dom/p #js {:className "bulk-import-success bulk-import-status"}
                                     (dom/span #js {:className "fa fa-check fa-2x"
                                                    :title (tr/translate ::bulk-import-success)})))
                            (dom/p #js {:className "bulk-import-failure bulk-import-status"}
                                   (dom/span #js {:className "fa fa-exclamation-triangle fa-2x"
                                                  :title (tr/translate ::bulk-import-failures)})))

                          (cond
                            (zero? progress)
                            (dom/div nil
                                     (dom/label #js {:className "field-label"}
                                                (tr/translate ::bulk-import-calculating)))

                            (= progress 1)
                            (dom/div nil
                                     (dom/label #js {:className "field-label"}
                                                (if (zero? (get-in data [:import-status :failed]))
                                                  (tr/translate ::bulk-import-complete)
                                                  (tr/translate ::bulk-import-complete-with-errors))))

                            :else
                            (dom/div nil
                                     (dom/label #js {:className "field-label"}
                                        (tr/translate ::estimated-time-remaining))
                             (dom/p nil "1:03:59")))))))))

(defn bulk-import-progress-component
  "Display the bulk import status, if available."
  [data owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (go-loop []
        (<! (timeout bulk-import-refresh-timeout))
        (rest/get-x-opts "/importer" {:success #(om/update! data :import-status (:body %))
                                      :suppress-error-dialog true})
        (recur)))
    om/IRender
    (render [_]
      (let [p (import-progress data)]
        (dom/div nil
                 (om/build bulk-import-details-panel-component data)
                 (dom/div #js {:className "bulk-import-progress"
                               :title (tr/translate ::bulk-import-status)}
                          (dom/span #js {:className "fa fa-upload"})
                          " "
                          (dom/span nil (int (* p 100)) "%")))))))

(defn nav-item-component
  "Render a list item for an item in the navigation bar."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (cond
        (= (:function data) "settings")
        (dom/li #js {:id "settings-nav" :className "icon-only"
                     :onClick #(toggle-settings!)}
                (dom/a nil (dom/span #js {:className "fa fa-cogs fa-2x"})))

        (= (:function data) "bulk-import-progress")
        (dom/li #js {:id "bulk-import-progress-nav"
                     :onClick #(om/transact! data :show-bulk-import-details not)}
                (dom/div #js {:className "bulk-import-progress-container"}
                         (om/build bulk-import-progress-component data)))

        :else
        (dom/li #js {:className (if (:experimental data) "experimental"
                                    (if (:deprecated data) "deprecated" ""))
                     :onClick #(nav/nav! (:url data))}
                (dom/a nil (:label data)))))))

(defn nav-component
  "Render navigation bar and contents."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (when (:loading data)
                 (dom/label #js {:className "loading"}
                            (dom/img #js {:src "images/spinner.gif" :height "32px"})
                            (:loading data)))
               (apply dom/ul #js {:className "nav navbar-nav"}
                      (om/build-all nav-item-component
                                    (if (:restricted-mode data)
                                      [nil]
                                      (remove nil? (:menu-items (:nav (:application data)))))))))))
