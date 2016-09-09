(ns camelot.component.survey.file
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [om.dom :as dom]
            [camelot.component.util :as util]
            [camelot.translation.core :as tr]
            [cljs.core.async :refer [<! chan >!]]
            [cljs-time.format :as tf]
            [camelot.nav :as nav]
            [camelot.util.cursorise :as cursorise])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def ^:private time-formatter (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(defn file-item-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item"}
               (dom/span #js {:className "menu-item-title"}
                         (:survey-file-name data))
               (dom/span #js {:className "menu-item-description"}
                         (dom/label nil (tr/translate ::upload-time)
                                    ": "
                                    (tf/unparse time-formatter (:survey-file-updated data))))))))

(defn upload-file
  [chan event]
  (let [f (aget (.. event -target -files) 0)]
    (rest/post-x-raw (str "/surveys/files")
                     [["survey-id" (state/get-survey-id)]
                      ["file" f]]
                     #(go (>! chan {:response (:body %)
                                    :success true}))
                     #(go (>! chan {:response (:body %) :success false})))))

(defn file-list-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [ch (om/get-state owner :chan)]
        (go
          (loop []
            (let [r (<! ch)]
              (if (:success r)
                (do
                  (rest/get-x (str "/surveys/" (:survey-id (:response r))
                                   "/files/" (:survey-file-id (:response r)))
                              (fn [resp]
                                (om/transact! data :files
                                              #(assoc % (get-in resp [:body :survey-file-id :value])
                                                      (cursorise/decursorise (:body resp))))))
                  (nav/analytics-event "file-upload" "success"))
                (do
                  ;; TODO notify user of error
                  (nav/analytics-event "file-upload" "failure"))))
            (recur)))))
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
               (dom/div #js {:className "simple-menu"}
                        (if (empty? (:files data))
                          (om/build util/blank-slate-component {}
                                    {:opts {:item-name (tr/translate ::item-name)
                                            :advice (tr/translate ::advice)}})
                          (dom/div nil
                                   (om/build-all file-item-component
                                                 (sort-by :survey-file-name (vals (:files data)))
                                                 {:key :survey-file-id}))))
               (dom/input #js {:type "file"
                               :onChange #(upload-file (:chan state) %)})))))

(defn file-menu-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-x (str "/surveys/" (state/get-survey-id) "/files")
                  #(om/update! data :files (reduce (fn [acc x] (assoc acc (:survey-file-id x) x))
                                                   {} (:body %)))))
    om/IRender
    (render [_]
      (when (:files data)
        (dom/div #js {:className "section"}
                 (om/build file-list-component data))))))
