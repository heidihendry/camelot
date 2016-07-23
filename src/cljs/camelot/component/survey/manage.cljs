(ns camelot.component.survey.manage
  (:require [om.core :as om]
            [camelot.nav :as nav]
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
                                                          (:trap-station-id data))))}
               (dom/span #js {:className "menu-item-title"}
                         (:trap-station-name data))
               (dom/span #js {:className "menu-item-description"}
                         (str "Latitude: " (:trap-station-latitude data)
                              " Longitude: " (:trap-station-longitude data)))))))

(def day-formatter (tf/formatter "yyyy-MM-dd"))

(def image-mime #"^image/.*")

(defn upload-file
  [sesscam-id file chan]
  (when (re-matches image-mime (.-type file))
    (rest/post-x-raw "/capture/upload" [["session-camera-id" sesscam-id]
                                        ["file" file]]
                     #(go (>! chan true)))))

(defn incomplete-deployment-list-component
  [data owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [n (om/get-node owner)]
        (.addEventListener n "dragenter" #(do (prn "DRAG")
                                              (.stopPropagation %)
                                              (.preventDefault %)))
        (.addEventListener n "dragover" #(do (prn "OVER")
                                             (.stopPropagation %)
                                             (.preventDefault %)))
        (.addEventListener n "drop" #(do
                                       (.stopPropagation %)
                                       (.preventDefault %)
                                       (let [fs (.. % -dataTransfer -files)
                                             upl-chan (chan)
                                             complete (atom 0)]
                                         (go
                                           (loop []
                                             (let [r (<! upl-chan)]
                                               (swap! complete inc)
                                               (prn @complete)
                                               (if (= (.-length fs) @complete)
                                                 (prn "All uploaded")
                                                 (recur)))))
                                         (doseq [idx (range (.-length fs))]
                                           (upload-file (:trap-station-session-camera-id data)
                                                        (aget fs idx)
                                                        upl-chan)))))))
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item extra-detailed"}
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
                        (tf/unparse day-formatter (:trap-station-session-end-date data))
                        " "
                        (dom/label nil " Camera Name: ")
                        " "
                        (:camera-name data))))))

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
                        (om/build-all incomplete-deployment-list-component
                                      (sort-by :trap-station-name
                                               (:incomplete-deployments data))
                                      {:key :trap-station-id}))))))

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
                                      {:key :trap-station-id}))
               (dom/div #js {:className "sep"})
               (dom/button #js {:className "btn btn-primary"
                                :onClick #(do (nav/nav! "/trap-stations/create")
                                              (nav/analytics-event "survey-deployment" "create-click"))
                                :disabled "disabled"
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
