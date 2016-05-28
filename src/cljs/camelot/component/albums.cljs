(ns camelot.component.albums
  (:require [camelot.component.nav :as nav]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [camelot.util :as util]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-time.format :as tf]))

(def day-formatter (tf/formatter "yyyy-MM-dd"))

(defn compare-validity
  "Predicate for comparing the severity of two problems."
  [val-a val-b]
  (let [ratings {:pass 0
                 :warn 1
                 :fail 2}]
    (< (get ratings (:result val-a)) (get ratings (:result val-b)))))

(defn compare-album-validity
  "Predicate for sort ordering on problem results."
  [[path-a alb-a] [path-b alb-b]]
  (let [sevs #(keys (group-by :result (:problems %)))
        ratings {:pass 0
                 :warn 1
                 :fail 2}]
    (< (reduce #(+ (get ratings %2) %1) 0 (sevs alb-a))
       (reduce #(+ (get ratings %2) %1) 0 (sevs alb-b)))))

(defn show-import-dialog
  [path]
  (om/update! (state/import-dialog-state) :path path)
  (om/update! (state/import-dialog-state) :visible true))

(defn timespan-component
  "Display details about the time span covered by an album."
  [data owner]
  (reify
    om/IRender
    (render [this]
      (let [path (first data)
            album (second data)
            start (:datetime-start (:metadata album))
            end (:datetime-end (:metadata album))
            checks (group-by :result (:problems album))]
        (dom/span nil
                  (cond
                    (:fail checks) (dom/span #js {:className "fa fa-2x fa-remove album-result failure-result"})
                    (:warn checks) (dom/span #js {:className "fa fa-2x fa-exclamation-triangle album-result warning-result"})
                    :else (dom/span #js {:className "fa fa-2x fa-check album-result success-result"}))
                  (if (or (nil? start) (nil? end))
                    (dom/label nil "Timespan information missing")
                    (dom/span nil
                              (dom/label nil (str ""
                                                  (util/nights-elapsed start end)
                                                  " nights"))
                              (dom/div #js {:className "date-range"}
                                       (str (tf/unparse day-formatter start)
                                            " — "
                                            (tf/unparse day-formatter end)))))
                  (dom/button #js {:className (cond
                                                (:fail checks) "btn btn-default import"
                                                (:warn checks) "btn btn-default import"
                                                :else "btn btn-primary import")
                                   :disabled (when (:fail checks) "disabled")
                                   :title (when (:fail checks)
                                            "Unable to import due to validation errors.")
                                   :onClick #(if (:warn checks)
                                               (when (js/confirm
                                                      "This folder may contain data with flaws. Importing it may compromise the accuracy of future analyses. Do you want to continue?")
                                                 (show-import-dialog path))
                                               (show-import-dialog path))}
                              "Import"))))))

(defn problem-component
  "Render a list item for a validation problem."
  [problem owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/span #js {:className "album-problem"}
                         (condp = (:result problem)
                           :fail (dom/span #js {:className "fa fa-remove album-result failure-result"})
                           :warn (dom/span #js {:className "fa fa-exclamation-triangle album-result warning-result"}))
                         (:reason problem))))))

(defn album-component
  "Render a list of validation problems."
  [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (if (empty? (:problems data))
                 (dom/label #js {:className "album-problem"}
                           (dom/span #js {:className "fa fa-check album-result success-result"})
                           "No problems found. Time to analyse!")
                 (apply dom/span nil
                        (om/build-all problem-component (sort compare-validity
                                                              (:problems data)))))))))

(defn albums-component
  "Render a list of albums and their validation results."
  [album owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "album-container"}
               (dom/div #js {:className "timespan"}
                        (om/build timespan-component album))
               (dom/div #js {:className "album-details"}
                        (dom/label nil (first album))
                        (om/build album-component (second album)))))))

(defn album-view-component
  "Render an album validation summary."
  [app owner]
  (reify
    om/IRender
    (render [_]
      (let [albums (sort (comparator compare-album-validity)
                         (sort-by first (vec (:albums app))))]
        (dom/div #js {:onClick nav/settings-hide!}
                 (dom/div #js {:className "validation-heading"}
                          (dom/h3 nil "Validation Results"))
                 (apply dom/div nil (om/build-all albums-component (into [] albums))))))))

(defn reload-albums
  "Reload the available albums"
  []
  (om/update! (state/app-state-cursor) :loading true)
  (rest/get-albums #(let [resp (:body %)]
                      (om/update! (state/app-state-cursor) :loading false)
                      (if (= (type resp) js/String)
                        (do
                          (nav/settings-show!)
                          (js/alert resp))
                        (do
                          (om/update! (state/app-state-cursor) :albums resp))))))
