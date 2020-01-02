(ns camelot.component.detector
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [camelot.rest :as rest]
   [camelot.util.misc :as misc]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]))

(defn- table-row
  [[label _ count] owner]
  (reify
    om/IRender
    (render [_]
      (dom/tr nil
       (dom/td nil label)
       (dom/td nil count)))))

(def ^:private refresh-time 2000)

(defn- describe-stats
  [app owner]
  (reify
    om/IInitState
    (init-state [_] {:loading? true})
    om/IDidMount
    (did-mount [_]
      (letfn [(fetch []
                (go
                  (let [resp (<! (http/get (misc/with-baseurl "/detector/status")))]
                    (when (= (:status resp) 200)
                      (om/set-state! owner :stats (js->clj (:body resp) true))
                      (om/set-state! owner :loading? false)))))]
        (fetch)
        (om/set-state! owner :interval
                       (.setInterval js/window fetch refresh-time))))
    om/IWillUnmount
    (will-unmount [_]
      (.clearInterval js/window (om/get-state owner :interval)))
    om/IRenderState
    (render-state [_ state]
      (if (:loading? state)
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))
        (let [stats (:events (:stats state))
              status (:system-status (:stats state))
              schema [["Suggestions added (high confidence)" [:media :result-high-confidence-suggestion-added]]
                      ["Suggestions added (low confidence)" [:media :result-low-confidence-suggestion-added]]
                      ["Media with suggestions" [:media :result-create-suggestions]]
                      ["Image batch results retrieved" [:task :poll-task-completed]]
                      ["Image batch creations completed" [:task :prepare-task-created]]
                      ["Image batch empty submissions" [:task :submit-no-completed-uploads]]
                      ["Image batch submissions completed" [:task :submit-task-call-success]]
                      ["Image batch archivals completed" [:task :archive-success]]
                      ["Image uploads completed" [:media :upload-succeeded]]
                      ["Image uploads skipped" [:media :upload-skipped]]]
              error-schema [["Suggestion creation failed" [:media :result-create-suggestion-failed]]
                            ["Image batch results failed" [:task :poll-task-failed]]
                            ["Image batch creations failed (retried)"
                             [:trap-station-session-camera :prepare-task-create-failed]]
                            ["Image batch submissions failed" [:task :submit-retry-limit-reached]]
                            ["Image batch archivals failed" [:task :archive-failed]]
                            ["Image uploads failed" [:media :upload-retry-limit-reached]]]]
          (dom/div nil
                   (dom/h4 nil "Activity")
                   (dom/span #js {:className "detector-status"}
                            (condp = status
                              "stopped"
                              (dom/span #js {:className "status-down"} "Stopped")

                              "paused"
                              (dom/span nil
                                        (dom/span #js {:className "status-paused"}
                                                  "Status: Paused")
                                        (dom/button #js {:className "btn btn-default"
                                                         :onClick #(rest/post-x "/detector/command"
                                                                                {:data {:cmd :resume}} identity)}
                                                    "▶"))

                              "detector-authentication-failed"
                              (dom/span #js {:className "status-down"} "Authentication failed")
                              "running"
                              (dom/span nil
                                        (dom/span #js {:className "status-up"}
                                                  "Status: Running")
                                        (dom/button #js {:className "btn btn-default"
                                                         :onClick #(rest/post-x "/detector/command"
                                                                                {:data {:cmd :pause}} identity)}
                                                    "⏸"))

                              (dom/span {:className "status-down"}
                                        status)))
                   (dom/table nil
                              (dom/thead nil
                                         (dom/tr #js {:className "table-heading"}
                                                 (dom/th nil "Event")
                                                 (dom/th nil "Count")))
                              (dom/tbody nil
                                         (om/build-all table-row
                                                       (mapv (fn [[l p]]
                                                               (vector l p (get-in stats p 0))) schema)
                                                       {:key-fn (fn [data]
                                                                  (-> data second second name))})))
                   (dom/br nil)
                   (dom/h4 nil "Errors")
                   (dom/table nil
                              (dom/thead nil
                                         (dom/tr #js {:className "table-heading"}
                                                 (dom/th nil "Event")
                                                 (dom/th nil "Count")))
                              (dom/tbody nil
                                         (om/build-all table-row
                                                       (mapv (fn [[l p]]
                                                               (vector l p (get-in stats p 0))) error-schema)
                                                       {:key-fn (fn [data]
                                                                  (-> data second second name))})))))))))

(defn stats-view
  "Detector stats."
  [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil "Detector"))
               (dom/div #js {:className "single-section text-section"}
                        (om/build describe-stats {}))))))


