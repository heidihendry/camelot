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
  (:import [goog.date DateTime])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def ^:private time-formatter (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(defn display-file-size
  [size]
  (cond
    (> size (* 1024 1024))
    (str (Math.round (/ size 1024 1024)) " MB")

    (> size 1024)
    (str (Math.round (/ size 1024)) " KB")

    :else
    (str size " B")))

(defn delete-file
  [state data event]
  (when (js/confirm (tr/translate ::confirm-delete))
    (rest/delete-x
     (str "/surveys/" (state/get-survey-id)
          "/files/" (:survey-file-id data))
     #(go (>! (:chan state) {:event :remove-file
                             :file data})))))

(defn file-item-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item detailed dynamic paddingless"}
               (dom/div #js {:className "pull-right fa fa-trash remove-file"
                             :onClick (partial delete-file state data)})
               (dom/a #js {:href (str "/surveys/" (state/get-survey-id)
                                      "/files/" (:survey-file-id data) "/download")
                           :className "menu-item-link"}
                      (dom/span #js {:className "menu-item-title"}
                                (:survey-file-name data))
                      (dom/div #js {:className "status pull-right"}
                               (dom/span nil
                                         (tf/unparse time-formatter
                                                     (DateTime.fromTimestamp
                                                      (.valueOf (:survey-file-updated data))))))
                      (dom/div #js {:className "menu-item-description"}
                               (dom/label nil (tr/translate ::file-size) ":")
                               " "
                               (dom/span nil
                                         (display-file-size
                                          (:survey-file-size data)))))))))

(defn upload-file
  [chan event]
  (let [f (aget (.. event -target -files) 0)]
    (rest/post-x-raw (str "/surveys/files")
                     [["survey-id" (state/get-survey-id)]
                      ["file" f]]
                     #(go (>! chan {:response (:body %)
                                    :success true}))
                     #(go (>! chan {:response (:body %) :success false})))))

(defn file-upload-component
  [data owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [ch (om/get-state owner :chan)]
        (go
          (loop []
            (let [r (<! ch)]
              (if (= (:event r) :remove-file)
                (om/transact! data :files #(dissoc % (get-in r [:file :survey-file-id])))
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
                    (nav/analytics-event "file-upload" "failure")))))
            (recur)))))
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
               (dom/div #js {:className "sep"})
               (dom/input #js {:type "file"
                               :ref "file-input"
                               :className "btn btn-primary file-input-field"
                               :onChange #(upload-file (:chan state) %)})))))

(defn file-list-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "simple-menu"}
               (if (empty? (:files data))
                 (om/build util/blank-slate-component {}
                           {:opts {:item-name (tr/translate ::item-name)
                                   :advice (tr/translate ::advice)}})
                 (dom/div nil
                          (om/build-all file-item-component
                                        (sort-by :survey-file-name (vals (:files data)))
                                        {:key :survey-file-id
                                         :init-state state})))))))

(defn file-menu-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (rest/get-x (str "/surveys/" (state/get-survey-id) "/files")
                  #(om/update! data :files (reduce (fn [acc x] (assoc acc (:survey-file-id x) x))
                                                   {} (:body %)))))
    om/IRenderState
    (render-state [_ state]
      (when (:files data)
        (dom/div #js {:className "section"}
                 (dom/div #js {:className "help-text"} (tr/translate ::help-text))
                 (om/build file-list-component data {:init-state state})
                 (om/build file-upload-component data
                           {:init-state state}))))))
