(ns camelot.component.nav
  (:require [camelot.nav :as nav]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.component.progress-bar :as progress-bar]
            [cljs.core.async :refer [<! chan >! timeout]]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [camelot.translation.core :as tr]
            [goog.date.duration :as duration]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def bulk-import-refresh-long-timeout 5000)
(def bulk-import-refresh-short-timeout 1000)

(defn time-elapsed
  [data]
  (let [total (- (.getTime (get-in data [:import-status :end-time]))
                 (.getTime (get-in data [:import-status :start-time])))
        sec (mod (/ total 1000) 60)
        fsec (.floor js/Math sec)]
    (str (duration/format total)
         " " fsec " "
         (if (= fsec 1)
           "second"
           "seconds"))))

(defn time-remaining-estimate
  [data progress]
  (let [dif (- (.getTime (get-in data [:import-status :end-time]))
               (.getTime (get-in data [:import-status :start-time])))
        todo (- (double (/ dif progress)) dif)
        sec (mod (/ todo 1000) 60)
        fsec (.floor js/Math sec)]
    (str (duration/format todo)
         " " fsec " "
         (if (= fsec 1)
           "second"
           "seconds"))))

(defn cancel-import
  [data]
  (rest/post-x-opts "/import/cancel" {}
                    {:successs #(do
                                  (om/update! data :cancelling-import true)
                                  (om/update! (state/bulk-import-state) :polling-active false))
                     :suppress-error-dialog true}))

(defn import-progress
  "Get the progress of the bulk import."
  [data]
  (double (/ (reduce + 0 (map (fn [[k v]] v) (select-keys (get-in data [:import-status :counts])
                                            [:complete :failed :ignored])))
             (max (reduce + 0 (map (fn [[k v]] v) (get-in data [:import-status :counts]))) 1))))

(defn bulk-import-details-panel-component
  [data owner]
  (reify
    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (when (= (import-progress data) 1)
        (om/update! (state/bulk-import-state) :polling-active false)))
    om/IRender
    (render [_]
      (let [total (reduce + 0 (map (fn [[k v]] v) (get-in data [:import-status :counts])))
            progress (import-progress data)]
        (dom/div #js {:id "bulk-import-details-panel"
                      :onClick #(do (.preventDefault %)
                                    (.stopPropagation %))
                      :className (if (and (pos-int? total)
                                          (:show-bulk-import-details data))
                        "expanded"
                        "")}
                 (when (:import-status data)
                   (dom/div #js {:className "bulk-import-details-container"}
                            (dom/span #js {:className "progress-numbers"}
                                      (reduce + 0 (map (fn [[k v]] v) (select-keys (get-in data [:import-status :counts])
                                                                     [:complete :failed :ignored])))
                                      " " (tr/translate :words/of-lc) " "
                                      total)
                            (dom/label #js {:className "field-label"}
                                       (tr/translate ::bulk-import-progress-label))
                            (dom/div #js {:className "bulk-import-progress-bar"}
                                     (om/build progress-bar/component data
                                               {:state (assoc (get-in data [:import-status :counts])
                                                              :total total)})
                                     (when-not (= progress 1)
                                       (dom/span #js {:className "fa fa-times-circle cancel-button"
                                                      :title (tr/translate :words/cancel)
                                                      :onClick #(cancel-import data)})))
                            (cond
                              (pos-int? (get-in data [:import-status :counts :ignored]))
                              (dom/p #js {:className "bulk-import-cancelled bulk-import-status"}
                                     (dom/span #js {:className "fa fa-ban fa-2x"
                                                    :title (tr/translate ::bulk-import-cancelled)}))

                              (pos-int? (get-in data [:import-status :counts :failed]))
                              (dom/p #js {:className "bulk-import-failure bulk-import-status"}
                                     (dom/span #js {:className "fa fa-exclamation-triangle fa-2x"
                                                    :title (tr/translate ::bulk-import-failures)}))

                              (= progress 1)
                              (dom/p #js {:className "bulk-import-success bulk-import-status"}
                                     (dom/span #js {:className "fa fa-check fa-2x"
                                                    :title (tr/translate ::bulk-import-success)})))

                            (cond
                              (zero? progress)
                              (dom/div nil
                                       (dom/label #js {:className "field-label"}
                                                  (tr/translate ::bulk-import-calculating)))

                              (pos-int? (get-in data [:import-status :counts :ignored]))
                              (dom/div nil
                                       (dom/label #js {:className "field-label"}
                                                  (tr/translate ::bulk-import-cancelled)))

                              (= progress 1)
                              (dom/div nil
                                       (dom/label #js {:className "field-label"}
                                                  (if (zero? (get-in data [:import-status :counts :failed]))
                                                    (tr/translate ::bulk-import-complete)
                                                    (tr/translate ::bulk-import-complete-with-errors)))
                                       (dom/p nil (time-elapsed data)))

                              :else
                              (dom/div nil
                                       (dom/label #js {:className "field-label"}
                                                  (tr/translate ::bulk-import-time-remaining))
                                       (dom/p nil (time-remaining-estimate data progress))))

                            (when (pos-int? (get-in data [:import-status :counts :failed]))
                              (dom/div nil
                                       (dom/label nil (tr/translate ::bulk-import-failed-paths))
                                       (dom/textarea #js {:rows "8"
                                                          :style #js {:width "100%"}}
                                                     (str/join "\n"
                                                               (get-in data [:import-status :failed-paths]))))))))))))

(defn bulk-import-progress-component
  "Display the bulk import status, if available."
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:cmd-chan (chan)})
    om/IDidMount
    (did-mount [_]
      (go-loop [tout bulk-import-refresh-long-timeout]
        (let [cch (om/get-state owner :cmd-chan)
              [msg port] (alts! [cch (timeout tout)])]
          (if (= port cch)
            (.log js/console "Bulk import poller cleaned up")
            (do
              (when (:polling-active (state/bulk-import-state))
                (rest/get-x-opts "/import" {:success #(om/update! data :import-status (:body %))
                                            :suppress-error-dialog true}))
              (let [p (import-progress @data)]
                (recur (if (and (pos? p) (< p 1))
                         bulk-import-refresh-short-timeout
                         bulk-import-refresh-long-timeout))))))))
    om/IWillUnmount
    (will-unmount [_]
      (go (>! (om/get-state owner :cmd-chan) {:cmd :stop})))
    om/IRender
    (render [_]
      (let [p (import-progress data)]
        (when (pos? p)
          (dom/div nil
                   (om/build bulk-import-details-panel-component data)
                   (dom/div #js {:className "bulk-import-progress"
                                 :title (tr/translate ::bulk-import-status)}
                            (dom/span #js {:className "fa fa-upload"})
                            " "
                            (dom/span nil (int (* p 100)) "%"))))))))

(defn render-dataset-option
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (name data)
                       :key data} (name data)))))

(defn dataset-selection-component
  [_ owner]
  (reify
    om/IInitState
    (init-state [_]
      {:dataset-ids []})
    om/IDidMount
    (did-mount [_]
      (rest/get-x "/dataset"
                  #(om/set-state! owner :dataset-ids (:dataset-ids (:body %)))))
    om/IRenderState
    (render-state [_ state]
      (let [options (:dataset-ids state)]
        (when (> (count options) 1)
          (dom/div nil
                   (dom/select #js {:value (first options)
                                    :title "Select a dataset"
                                    :onChange #(let [dataset-id (.. % -target -value)]
                                                 (rest/post-x (str "/dataset/select/" dataset-id) {}
                                                              (fn [_] (.. js/window -location reload))))}
                               (om/build-all render-dataset-option options))))))))

(defn nav-item-component
  "Render a list item for an item in the navigation bar."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (cond
        (= (:function data) "bulk-import-progress")
        (dom/li #js {:id "bulk-import-progress-nav"
                     :onMouseOver #(om/update! data :show-bulk-import-details true)
                     :onMouseOut #(om/update! data :show-bulk-import-details false)}
                (dom/div #js {:className "bulk-import-progress-container"}
                         (om/build bulk-import-progress-component data)))

        (= (:function data) "dataset-selection")
        (dom/li #js {:id "dataset-selection-nav"}
                (om/build dataset-selection-component data))

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
