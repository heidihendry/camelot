(ns camelot.component.deployment.recent
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.util.capture :as capture]
            [camelot.component.survey.create :as create]
            [om.dom :as dom]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [cljs-time.format :as tf])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def ^:private day-formatter (tf/formatter "yyyy-MM-dd"))
(def ^:private help-text
  "You can drag and drop capture files on a Camera Check to upload them.")

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
  (str "'" (.-name file) "' is not in a supported format.\n"))

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
  (let [reason (str "' " (or err "error during upload") "\n")
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
    (om/set-state! owner {:total (.-length files)
                          :complete 0
                          :failed 0
                          :errors nil
                          :ignored (- (.-length files) uploadable)})
    (nav/analytics-event "capture-upload" "upload-init")
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
                    :onClick #(om/update! (state/display-state) :error (:errors state))}
               (dom/div #js {:className "menu-item-title"}
                        (:trap-station-name data))
               (dom/div #js {:className "menu-item-description"}
                        (dom/label nil "GPS Coordinates:")
                        " " (:trap-station-latitude data) ", " (:trap-station-longitude data))
               (dom/div #js {:className "menu-item-description"}
                        (dom/label nil " Date:")
                        " "
                        (tf/unparse day-formatter (:trap-station-session-start-date data))
                        " -- "
                        (tf/unparse day-formatter (:trap-station-session-end-date data)))
               (dom/div #js {:className "menu-item-description"}
                        (dom/label nil " Camera Name: ") " " (:camera-name data))
               (when-not (or (zero? (get state :total)) (nil? (get state :total)))
                 (dom/div #js {:className "progress-bar-container"
                               :title (str (get state :complete) " complete, "
                                           (get state :failed) " failed and "
                                           (get state :ignored) " ignored")}
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
                 (dom/div nil "Show details"))))))

(defn recent-deployment-section-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource (str "/deployment/survey/"
                              (get-in (state/app-state-cursor)
                                      [:selected-survey :survey-id :value])
                              "/recent")
                         #(om/update! data :recent-deployments (:body %))))
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (dom/div #js {:className "help-text"}
                                 help-text)
                        (om/build-all recent-deployment-list-component
                                      (sort-by :trap-station-name
                                               (:recent-deployments data))
                                      {:key :trap-station-session-camera-id}))))))