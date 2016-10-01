(ns camelot.component.survey.bulk-import
  (:require [cljs.core.async :refer [chan]]
            [om.core :as om]
            [camelot.translation.core :as tr]
            [camelot.component.upload :as upload]
            [om.dom :as dom]))

(defn upload-success-handler
  [r]
  (prn "success"))

(defn bulk-import-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "section"}
               (dom/a #js {:href "/surveys/bulkimport/template"}
                      (dom/button #js {:className "btn btn-primary"}
                                  (tr/translate ::download)))
               (om/build upload/file-upload-component data
                           {:init-state state
                            :opts {:analytics-event "mapping-upload"
                                   :success-handler upload-success-handler
                                   :endpoint "/surveys/bulkimport/columnmap"}})))))
