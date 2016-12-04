(ns camelot.component.upload
  (:require [cljs.core.async :refer [<! chan >!]]
            [camelot.nav :as nav]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [om.dom :as dom]
            [om.core :as om])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn upload-file
  [ep chan event]
  (let [f (aget (.. event -target -files) 0)]
    (go (>! chan {:status :pending}))
    (rest/post-x-raw ep
                     [["survey-id" (state/get-survey-id)]
                      ["file" f]]
                     #(go (>! chan {:status :complete
                                    :response (:body %)
                                    :success true}))
                     #(go (>! chan {:status :complete
                                    :response (:body %)
                                    :success false})))))

(defn file-upload-component
  [data owner {:keys [analytics-event success-handler
                      failure-handler pending-handler endpoint]}]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [ch (om/get-state owner :chan)]
        (go
          (loop []
            (let [r (<! ch)]
              (if (= (:event r) :remove-file)
                (om/transact! data :files #(dissoc % (get-in r [:file :survey-file-id])))
                (case (:status r)
                  :pending (and pending-handler (pending-handler r))
                  :complete (if (:success r)
                              (do
                                (and success-handler (success-handler r))
                                (nav/analytics-event analytics-event "success"))
                              (do
                                (and failure-handler (failure-handler r))
                                (nav/analytics-event analytics-event "failure"))))))
            (recur)))))
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
               (dom/input #js {:type "file"
                               :ref "file-input"
                               :className "btn btn-primary file-input-field"
                               :onChange #(upload-file endpoint (:chan state) %)})))))
