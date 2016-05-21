(ns camelot.component.analysis
  (:require [camelot.util :as util]
            [smithy.util :as sutil]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]))

(defn analysis-view-component [app owner]
  "Render the analysis screen."
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div nil
                        (dom/h4 nil "Analysis Exports"))
               (dom/a #js {:href (util/with-baseurl "/maxent") :target "_blank"
                           :onClick #(sutil/analytics-event "maxent" "export")}
                      (dom/button #js {:className "btn btn-primary fa fa-download fa-2x"}
                                  " MaxEnt Export"))))))
