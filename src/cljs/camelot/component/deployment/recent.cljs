(ns camelot.component.deployment.recent
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.util.capture :as capture]
            [camelot.component.deployment.shared :as shared]
            [camelot.component.survey.create :as create]
            [camelot.component.progress-bar :as progress-bar]
            [om.dom :as dom]
            [cljs.core.async :refer [<! chan >! put!]]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [cljs-time.format :as tf]
            [camelot.component.util :as util]
            [camelot.translation.core :as tr])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def ^:private day-formatter (tf/formatter "yyyy-MM-dd"))
(def ^:private help-text (tr/translate ::help-text))

(defn delete
  "Delete the session camera and trigger a removal event."
  [state data event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (js/confirm (if (:has-uploaded-media data)
                      (tr/translate ::confirm-delete-has-media)
                      (tr/translate ::confirm-delete)))
    (rest/delete-x (str "/trap-station-session-cameras/" (:trap-station-session-camera-id data))
                   #(go (>! (:chan state) {:event :delete :data data})))))

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

(defonce signal-detector-timeout (atom nil))

(defn- upload-files
  [sesscam-id owner fqueue result-chan]
  (let [req-chan (chan)]
    (go
      (loop []
        (let [{:keys [remaining]} (<! req-chan)
              file (peek remaining)]
          (if (is-uploadable? file)
            (rest/post-x-raw "/import/upload" [["session-camera-id" sesscam-id]
                                               ["file" file]]
                             #(do (let [err (:error (:body %))]
                                    (put! result-chan {:file file
                                                       :success (nil? err)
                                                       :error err})
                                    (put! req-chan {:remaining (pop remaining)})))
                             #(do (put! result-chan {:file file :success false})
                                  (put! req-chan {:remaining (pop remaining)})))
            (add-upload-problem owner ["skipped" (.-type file)]
                                (unsupported-str file)))
          (if (> (count remaining) 1)
            (recur)
            (do
              (when @signal-detector-timeout
                (swap! signal-detector-timeout #(.clearTimeout js/window %)))
              (reset! signal-detector-timeout
                      (.setTimeout js/window
                                   #(do
                                      (rest/post-x "/detector/command" {:data {:cmd :rerun}} identity)
                                      (reset! signal-detector-timeout nil))
                                   5000)))))))
    (put! req-chan {:remaining fqueue})))

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
    (let [fqueue (into #queue [] (.from js/Array files))]
      (upload-files (:trap-station-session-camera-id data)
                    owner
                    fqueue
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
        (om/set-state! owner :total 0)
        (om/set-state! owner :complete 0)
        (om/set-state! owner :failed 0)
        (om/set-state! owner :errors nil)
        (om/set-state! owner :ignored 0)
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
      (dom/div #js {:className "menu-item detailed dynamic no-click"
                    :onClick #(om/update! (state/display-state) [:notification :info] (:errors state))}
               (dom/div #js {:className "pull-right fa fa-times remove top-corner"
                             :onClick (partial delete state data)})
               (when (:has-uploaded-media data)
                 (dom/span #js {:className "status pull-right"}
                           (tr/translate ::media-uploaded)))
               (dom/div #js {:className "menu-item-title"}
                        (:camera-name data) " " (tr/translate :words/at-lc)" "
                        (:trap-station-name data))
               (when (and (:total state)
                          (> (:total state) 0)
                          (= (+ (:ignored state) (:failed state) (:complete state)) (:total state)))
                 (dom/span #js {:className "pull-right fa fa-check fa-3x green"}))
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
               (om/build progress-bar/component data
                         {:state (select-keys state [:total :ignored :complete :failed])})
               (when-not (zero? (+ (get state :ignored) (get state :failed)))
                 (dom/div #js {:className "pointer"} (tr/translate ::show-details)))))))

(defn recent-deployment-section-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! data :recent-deployments nil))
    om/IDidMount
    (did-mount [_]
      (om/update! data :deployment-sort-order :trap-station-session-end-date)
      (rest/get-resource (str "/camera-deployment/survey/"
                              (get-in (state/app-state-cursor)
                                      [:selected-survey :survey-id :value])
                              "/recent")
                         #(om/update! data :recent-deployments (:body %)))
      (let [ch (om/get-state owner :chan)]
        (go
          (loop []
            (let [r (<! ch)]
              (cond
                (= (:event r) :delete)
                (om/transact! data :recent-deployments #(remove (fn [x] (= x (:data r))) %))))
            (recur)))))
    om/IRenderState
    (render-state [_ state]
      (if (nil? (:recent-deployments data))
        (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"}))
        (dom/div #js {:className "section"}
                 (dom/div #js {:className "simple-menu"}
                          (if (empty? (:recent-deployments data))
                            (om/build util/blank-slate-component {}
                                      {:opts {:item-name (tr/translate ::blank-item-name)
                                              :advice (tr/translate ::blank-advice)}})
                            (dom/div nil
                                     (dom/div #js {:className "help-text"} help-text)
                                     (om/build shared/deployment-sort-menu data {:opts {:show-end-date true}})
                                     (om/build-all recent-deployment-list-component
                                                   (sort (shared/deployment-sorters (get data :deployment-sort-order))
                                                         (:recent-deployments data))
                                                   {:key :trap-station-session-camera-id
                                                    :init-state state})))))))))
