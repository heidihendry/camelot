(ns camelot.component.deployment.shared
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.translation.core :as tr])
  (:import [goog.date UtcDateTime]
           [goog.date DateTime]
           [goog.i18n DateTimeFormat]))

(defn datetime-in-future?
  "Predicate indicating whether the datetime is in the future.  False if datetime is nil."
  [datetime]
  (let [now (.getTime (DateTime.))
        ms-today (mod now (* 24 60 60 1000))
        start-of-day (- now ms-today (* 60 1000 (.getTimezoneOffset (DateTime.))))]
    (and datetime (> (.getTime datetime) start-of-day))))

(def deployment-sorters
  {:trap-station-name (comparator (fn [a b]
                                    (< (compare (:trap-station-name a)
                                                (:trap-station-name b))
                                       0)))
   :trap-station-session-start-date (comparator
                                     (fn [a b]
                                       (cond
                                         (nil? (:trap-station-session-start-date b)) false
                                         (nil? (:trap-station-session-start-date a)) true
                                         :else (> (.getTime (:trap-station-session-start-date a))
                                                  (.getTime (:trap-station-session-start-date b))))))
   :trap-station-session-end-date (comparator
                                     (fn [a b]
                                       (cond
                                         (nil? (:trap-station-session-end-date b)) false
                                         (nil? (:trap-station-session-end-date a)) true
                                         :else (> (.getTime (:trap-station-session-end-date a))
                                                  (.getTime (:trap-station-session-end-date b))))))})

(defn deployment-sort-menu
  [data owner {:keys [show-end-date]}]
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
                           (if show-end-date
                             (tr/translate ::start-date)
                             (tr/translate :words/date)))
               " "
               (when show-end-date
                 (dom/button #js {:className (str "btn btn-default btn-sml"
                                                  (if (= (:deployment-sort-order data) :trap-station-session-end-date)
                                                    " active"
                                                    ""))
                                  :onClick #(om/update! data :deployment-sort-order :trap-station-session-end-date)}
                             (tr/translate ::end-date)))))))
