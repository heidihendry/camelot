(ns camelot.component.deployment.recent
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.util.capture :as capture]
            [camelot.component.deployment.shared :as shared]
            [camelot.component.survey.create :as create]
            [om.dom :as dom]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [cljs-time.format :as tf]
            [camelot.component.util :as util]
            [camelot.translation.core :as tr])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def ^:private day-formatter (tf/formatter "yyyy-MM-dd"))
(def ^:private help-text (tr/translate ::help-text))

(defn- add-upload-problem
  [owner event-details desc]
  (apply (partial nav/analytics-event "capture-upload") event-details)
  (if (nil? (om/get-state owner :errors))
    (om/set-state! owner :errors desc)
    (om/update-state! owner :errors #(str % desc))))

(defn- is-uploadable?
  [file]
  (some #(= (.-type file) %) (keys capture/image-mimes)))

(defn- unsupported-str
  [file]
  (str (tr/translate ::format-not-supported (.-name file)) "\n"))

(defn- upload-file
  [sesscam-id owner file chan]
  (if (is-uploadable? file)
    (rest/post-x-raw "/capture/upload" [["session-camera-id" sesscam-id]
                                        ["file" file]]
                     #(go (let [err (:error (:body %))]
                            (>! chan {:file file
                                      :success (nil? err)
                                      :error err})))
                     #(go (>! chan {:file file :success false})))
    (add-upload-problem owner ["skipped" (.-type file)]
                        (unsupported-str file))))

(defn- uploadable-count
  [fs]
  (reduce #(if (is-uploadable? (aget fs %2))
             (inc %1)
             %1) 0
             (range (.-length fs))))

(defn- display-upload-failure
  [owner f err]
  (let [reason (str "' " (or err (tr/translate ::upload-error)) "\n")
        desc (str "'" (.-name f) reason)]
    (add-upload-problem owner ["failed" (.-type f)] desc)))

(defn- handle-upload-failure
  [owner f err]
  (display-upload-failure owner f err)
  (om/update-state! owner :failed inc))

(defn- percent-of
  [data k]
  (* 100 (/ (get data k) (get data :total))))

(defn- complete-percent
  [data]
  (percent-of data :complete))

(defn- ignored-percent
  [data]
  (percent-of data :ignored))

(defn- failed-percent
  [data]
  (percent-of data :failed))

(defn- drop-file-handler
  [data owner files]
  (let [uploadable (uploadable-count files)
        upl-chan (chan)]
    (nav/analytics-event "capture-upload" "upload-init")
    (om/update-state! owner :total #(+ % (.-length files)))
    (om/update-state! owner :ignored #(+ % (- (.-length files) uploadable)))
    (go
      (loop []
        (let [r (<! upl-chan)]
          (if (:success r)
            (do
              (om/update-state! owner :complete inc)
              (nav/analytics-event "capture-upload" "success" (.-type (:file r))))
            (handle-upload-failure owner (:file r) (:error r)))
          (recur))))
    (doseq [idx (range (.-length files))]
      (upload-file (:trap-station-session-camera-id data)
                   owner
                   (aget files idx)
                   upl-chan))))

(defn recent-deployment-list-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:total 0 :ignored 0 :complete 0 :failed 0 :errors nil})
    om/IDidMount
    (did-mount [_]
      (let [n (om/get-node owner)]
        (om/set-state! owner {:total 0
                              :complete 0
                              :failed 0
                              :errors nil
                              :ignored 0})
        (.addEventListener n "dragenter"
                           #(do (.stopPropagation %) (.preventDefault %)))
        (.addEventListener n "dragover"
                           #(do (.stopPropagation %) (.preventDefault %)))
        (.addEventListener n "drop"
                           #(do
                              (.stopPropagation %)
                              (.preventDefault %)
                              (let [fs (.. % -dataTransfer -files)]
                                (drop-file-handler data owner fs))))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item detailed dynamic"
                    :onClick #(om/update! (state/display-state) [:notification :info] (:errors state))}
               (dom/div #js {:className "menu-item-title"}
                        (:camera-name data) " " (tr/translate :words/at-lc)" "
                        (:trap-station-name data))
               (dom/div #js {:className "menu-item-description"}
                        (dom/label nil " " (tr/translate :words/date) ":")
                        " "
                        (tf/unparse day-formatter (:trap-station-session-start-date data))
                        " " (tr/translate :words/to-lc) " "
                        (tf/unparse day-formatter (:trap-station-session-end-date data)))
               (dom/div #js {:className "menu-item-description"}
                        (dom/label nil (tr/translate ::gps-coordinates) ":")
                        " "
                        (:trap-station-latitude data) ", " (:trap-station-longitude data))
               (when-not (or (zero? (get state :total)) (nil? (get state :total)))
                 (dom/div #js {:className "progress-bar-container"
                               :title (tr/translate ::progress-bar-title
                                                    (get state :complete)
                                                    (get state :failed)
                                                    (get state :ignored))}
                          (dom/div #js {:className "progress-bar"})
                          (dom/div #js {:className "progress-bar-state"
                                        :style #js {:width (str (complete-percent state) "%")}})
                          (dom/div #js {:className "ignored-bar-state"
                                        :style #js {:left (str (complete-percent state) "%")
                                                    :width (str (ignored-percent state) "%")}})
                          (dom/div #js {:className "error-bar-state"
                                        :style #js {:left (str (- 100 (failed-percent state)) "%")
                                                    :width (str (failed-percent state) "%")}})))
               (when-not (zero? (+ (get state :ignored) (get state :failed)))
                 (dom/div nil (tr/translate ::show-details)))))))

(defn recent-deployment-section-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (or (:deployment-sort-order data)
          (om/update! data :deployment-sort-order :trap-station-name))
      (rest/get-resource (str "/deployment/survey/"
                              (get-in (state/app-state-cursor)
                                      [:selected-survey :survey-id :value])
                              "/recent")
                         #(om/update! data :recent-deployments (:body %))))
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (if (empty? (:recent-deployments data))
                          (om/build util/blank-slate-component {}
                                    {:opts {:item-name (tr/translate ::blank-item-name)
                                            :advice (tr/translate ::blank-advice)}})
                          (dom/div nil
                                   (dom/div #js {:className "help-text"} help-text)
                                   (om/build shared/deployment-sort-menu data)
                                   (om/build-all recent-deployment-list-component
                                                 (sort (shared/deployment-sorters (get data :deployment-sort-order))
                                                       (:recent-deployments data))
                                                 {:key :trap-station-session-camera-id}))))))))
