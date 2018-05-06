(ns camelot.component.survey.file
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [om.dom :as dom]
            [camelot.component.util :as util]
            [camelot.component.upload :as upload]
            [camelot.translation.core :as tr]
            [cljs.core.async :refer [<! chan >!]]
            [cljs-time.format :as tf]
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
     (str "/files/survey/" (state/get-survey-id)
          "/file/" (:survey-file-id data))
     #(go (>! (:chan state) {:event :remove-file
                             :file data})))))

(defn file-item-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item detailed dynamic paddingless"}
               (dom/div #js {:className "pull-right fa fa-times remove top-corner surveyfile"
                             :onClick (partial delete-file state data)})
               (dom/a #js {:href (str "/files/survey/" (state/get-survey-id)
                                      "/file/" (:survey-file-id data) "/download")
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
                                        (sort-by :survey-file-name (map (fn [[k v]] v) (:files data)))
                                        {:key :survey-file-id
                                         :init-state state})))))))

(defn upload-success-handler
  [data r]
  (rest/get-x (str "/file/survey/" (:survey-id (:response r))
                   "/file/" (:survey-file-id (:response r)))
              (fn [resp]
                (om/transact! data :files
                              #(assoc % (get-in resp [:body :survey-file-id :value])
                                      (cursorise/decursorise (:body resp)))))))

(defn file-menu-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! data :files ""))
    om/IDidMount
    (did-mount [_]
      (rest/get-x (str "/files/survey/" (state/get-survey-id))
                  #(om/update! data :files (reduce (fn [acc x] (assoc acc (:survey-file-id x) x))
                                                   {} (:body %)))))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! data :files ""))
    om/IRenderState
    (render-state [_ state]
      (if (:files data)
        (dom/div #js {:className "section"}
                 (om/build file-list-component data {:init-state state})
                 ;; TODO notify user of error
                 (dom/div #js {:className "sep"})
                 (om/build upload/file-upload-component data
                           {:init-state state
                            :opts {:analytics-event "file-upload"
                                   :success-handler (partial upload-success-handler data)
                                   :endpoint "/files"}}))
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))))))
