(ns camelot.component.survey.manage
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

(defn action-item-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className (str "menu-item"
                                    (if (:active data) " active" ""))
                    :onClick #(do
                                (go (>! (:active-chan state) (:action data)))
                                (nav/analytics-event "survey"
                                                     (str (name (:action data)) "-click")))}
               (dom/span #js {:className "menu-item-title"}
                         (:name data))))))

(defn action-menu-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:active-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [chan (om/get-state owner :active-chan)]
        (go
          (loop []
            (let [r (<! chan)]
              (om/update! data :active r)
              (doseq [m (:menu data)]
                (om/update! m :active (= (:action m) r)))
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "section simple-menu"}
               (om/build-all action-item-component
                             (:menu data)
                             {:key :action
                              :init-state state})))))

(defn deployment-list-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed"
                    :onClick #(do
                                (nav/analytics-event "survey-deployment" "trap-station-click")
                                (nav/nav! (nav/survey-url "deployments"
                                                          (:trap-station-session-id data))))}
               (dom/span #js {:className "menu-item-title"}
                         (:trap-station-name data))
               (dom/span #js {:className "menu-item-description"}
                         (str "Latitude: " (:trap-station-latitude data)
                              " Longitude: " (:trap-station-longitude data)))))))

(def day-formatter (tf/formatter "yyyy-MM-dd"))

(defn add-upload-problem
  [owner event-details desc]
  (apply (partial nav/analytics-event "capture-upload") event-details)
  (if (nil? (om/get-state owner :errors))
      (om/set-state! owner :errors desc)
      (om/update-state! owner :errors #(str % desc))))

(defn is-uploadable?
  [file]
  (some #(= (.-type file) %) (keys capture/image-mimes)))

(defn upload-file
  [sesscam-id owner file chan]
  (if (is-uploadable? file)
    (rest/post-x-raw "/capture/upload" [["session-camera-id" sesscam-id]
                                        ["file" file]]
                     #(go (let [err (:error (:body %))]
                            (if err
                              (>! chan {:file file :success false :error err})
                              (>! chan {:file file :success true}))))
                     #(go (>! chan {:file file :success false})))
    (add-upload-problem owner ["skipped" (.-type file)]
                        (str "'" (.-name file) "' is not in a supported format.\n"))))

(defn uploadable-count
  [fs]
  (reduce #(if (is-uploadable? (aget fs %2))
             (inc %1)
             %1) 0
             (range (.-length fs))))

(defn display-upload-failure
  [owner f err]
  (let [reason (if err
                 (str " " err "\n")
                 "' error during upload\n")
        desc (str "'" (.-name f) reason)]
    (add-upload-problem owner ["failed" (.-type f)] desc)))

(defn handle-upload-failure
  [owner f err]
  (display-upload-failure owner f err)
  (om/update-state! owner :failed inc))

(defn incomplete-deployment-list-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:total 0 :ignored 0 :complete 0 :failed 0 :errors nil})
    om/IDidMount
    (did-mount [_]
      (let [n (om/get-node owner)]
        (.addEventListener n "dragenter" #(do (.stopPropagation %)
                                              (.preventDefault %)))
        (.addEventListener n "dragover" #(do (.stopPropagation %)
                                             (.preventDefault %)))
        (.addEventListener n "drop" #(do
                                       (.stopPropagation %)

                                       (.preventDefault %)
                                       (let [fs (.. % -dataTransfer -files)
                                             uploadable (uploadable-count fs)
                                             upl-chan (chan)
                                             complete (atom 0)]
                                         (om/set-state! owner :total (.-length fs))
                                         (om/set-state! owner :complete 0)
                                         (om/set-state! owner :failed 0)
                                         (om/set-state! owner :errors nil)
                                         (om/set-state! owner :ignored (- (.-length fs) uploadable))
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
                                         (doseq [idx (range (.-length fs))]
                                           (upload-file (:trap-station-session-camera-id data)
                                                        owner
                                                        (aget fs idx)
                                                        upl-chan)))))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item extra-detailed"
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
                                        :style #js {:width (str (* 100 (/ (get state :complete)
                                                                          (get state :total))) "%")}})
                          (dom/div #js {:className "ignored-bar-state"
                                        :style #js {:left (str (* 100 (/ (get state :complete)
                                                                          (get state :total))) "%")
                                                    :width (str (* 100 (/ (get state :ignored)
                                                                          (get state :total))) "%")}})
                          (dom/div #js {:className "error-bar-state"
                                        :style #js {:left (str (* 100 (/ (+ (get state :complete) (get state :ignored))
                                                                         (get state :total))) "%")
                                                    :width (str (* 100 (/ (get state :failed)
                                                                          (get state :total))) "%")}})))
               (when-not (zero? (+ (get state :ignored) (get state :failed)))
                 (dom/div nil "Click for details."))))))

(defn incomplete-deployment-section-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource (str "/deployment/survey/"
                              (get-in (state/app-state-cursor)
                                      [:selected-survey :survey-id :value])
                              "/incomplete")
                         #(om/update! data :incomplete-deployments (:body %))))
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (dom/div #js {:className "help-text"}
                                 "You can drag and drop capture files on a Camera Check to upload them.")
                        (om/build-all incomplete-deployment-list-component
                                      (sort-by :trap-station-name
                                               (:incomplete-deployments data))
                                      {:key :trap-station-session-camera-id}))))))

(defn deployment-section-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource (str "/deployment/survey/"
                              (get-in (state/app-state-cursor)
                                      [:selected-survey :survey-id :value]))
                         #(om/update! data :trap-stations (:body %))))
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (om/build-all deployment-list-component
                                      (sort-by :trap-station-name (:trap-stations data))
                                      {:key :trap-station-session-id}))
               (dom/div #js {:className "sep"})
               (dom/button #js {:className "btn btn-primary"
                                :onClick #(do (nav/nav! (str "/"
                                                             (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])
                                                             "/deployments/create"))
                                              (nav/analytics-event "survey-deployment" "create-click"))
                                :title "Add a new deployment"}
                           (dom/span #js {:className "fa fa-plus"})
                           " Add Deployment")))))

(defn survey-section-containers-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className "section-container"}
                        (om/build action-menu-component data)
                        (dom/button #js {:className "btn btn-default view-library"
                                         :onClick #(do (nav/analytics-event "survey"
                                                                            "view-library-click")
                                                       (nav/nav! (str "/" (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])
                                                                      "/library")))}
                                    (dom/span #js {:className "fa fa-book"})
                                    " Survey Library"))
               (dom/div #js {:className "section-container"}
                        (case (:active data)
                          :deployment (om/build deployment-section-component data)
                          :upload (om/build incomplete-deployment-section-component data)
                          ""))))))

(defn survey-management-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil (get-in (state/app-state-cursor) [:selected-survey :survey-name :value])))
               (dom/div nil (om/build survey-section-containers-component data))))))
