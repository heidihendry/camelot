(ns camelot.component.util
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.translation.core :as tr]))

(defn blank-slate-component
  [data owner {:keys [item-name advice notice]}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "blank-slate"}
               (dom/div #js {:className "large"}
                        (or notice
                            (tr/translate ::blank-notice-template item-name)))
               (dom/div #js {:className "advice"}
                        (or advice
                            (tr/translate ::use-button-below)))))))

(defn blank-slate-beta-component
  [data owner {:keys [item-name]}]
  (reify
    om/IRender
    (render [_]
      (om/build blank-slate-component {}
                {:opts {:item-name item-name
                        :advice (tr/translate ::use-advanced-menu)}}))))
