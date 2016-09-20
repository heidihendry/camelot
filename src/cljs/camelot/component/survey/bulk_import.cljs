(ns camelot.component.survey.bulk-import
  (:require [om.core :as om]
            [camelot.translation.core :as tr]
            [om.dom :as dom]))

(defn bulk-import-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/a #js {:href "/surveys/bulkimport/template"}
                      (dom/button #js {:className "btn btn-primary"}
                                  (tr/translate ::download)))))))
