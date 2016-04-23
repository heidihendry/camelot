(ns camelot.component.analysis
  (:require [camelot.util :as util]
            [camelot.nav :as nav]
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
      (dom/a #js {:href (util/with-baseurl "/maxent") :target "_blank"}
             (dom/button #js {:className "btn btn-primary"}
                         "MaxEnt Export")))))
