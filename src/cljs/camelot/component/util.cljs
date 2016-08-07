(ns camelot.component.util
  (:require [om.core :as om]
            [om.dom :as dom]))

(defn blank-slate-component
  [data owner {:keys [item-name advice notice]}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "blank-slate"}
               (dom/div #js {:className "large"}
                        (or notice
                            (str "There aren't any " item-name " yet")))
               (dom/div #js {:className "advice"}
                        (or advice
                            "You can set some up using the button below"))))))

(defn blank-slate-beta-component
  [data owner {:keys [item-name]}]
  (reify
    om/IRender
    (render [_]
      (om/build blank-slate-component {}
                {:opts {:item-name item-name
                        :advice "You can set some up using the 'Advanced' menu below"}}))))
