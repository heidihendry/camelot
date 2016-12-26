(ns camelot.component.progress-bar
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.translation.core :as tr]))

(defn- percent-of
  [data k]
  (* 100 (/ (get data k) (get data :total))))

(defn- complete-percent
  [data]
  (percent-of data :complete))

(defn- ignored-percent
  [data]
  (percent-of data :ignored))

(defn- failed-percent
  [data]
  (percent-of data :failed))

(defn component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:total 0 :ignored 0 :complete 0 :failed 0})
    om/IRenderState
    (render-state [_ state]
      (when (and (get state :total) (> (get state :total) 0))
        (dom/div #js {:className "progress-bar-container"
                      :title (tr/translate ::progress-bar-title
                                           (get state :complete)
                                           (get state :failed)
                                           (get state :ignored))}
                 (dom/div #js {:className "progress-bar"})
                 (dom/div #js {:className "progress-bar-state"
                               :style #js {:width (str (complete-percent state) "%")}})
                 (dom/div #js {:className "ignored-bar-state"
                               :style #js {:left (str (complete-percent state) "%")
                                           :width (str (ignored-percent state) "%")}})
                 (dom/div #js {:className "error-bar-state"
                               :style #js {:left (str (- 100 (failed-percent state)) "%")
                                           :width (str (failed-percent state) "%")}}))))))
