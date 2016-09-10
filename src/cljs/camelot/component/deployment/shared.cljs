(ns camelot.component.deployment.shared
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.translation.core :as tr]))

(def deployment-sorters
  {:trap-station-name (comparator (fn [a b]
                                    (< (compare (:trap-station-name a)
                                                (:trap-station-name b))
                                       0)))
   :trap-station-session-start-date (comparator
                                     (fn [a b]
                                       (cond
                                         (nil? (:trap-station-session-start-date b)) true
                                         (nil? (:trap-station-session-start-date a)) false
                                         :else (< (.getTime (:trap-station-session-start-date a))
                                                  (.getTime (:trap-station-session-start-date b))))))})

(defn deployment-sort-menu
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "field-label"} (tr/translate ::sort-by) ":") " "
               (dom/button #js {:className (str "btn btn-default btn-sml"
                                                (if (= (:deployment-sort-order data) :trap-station-name)
                                                  " active"
                                                  ""))
                                :onClick #(om/update! data :deployment-sort-order :trap-station-name)}
                           (tr/translate :words/name))
               " "
               (dom/button #js {:className (str "btn btn-default btn-sml"
                                                (if (= (:deployment-sort-order data) :trap-station-session-start-date)
                                                  " active"
                                                  ""))
                                :onClick #(om/update! data :deployment-sort-order :trap-station-session-start-date)}
                           (tr/translate :words/date))))))
