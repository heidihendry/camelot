(ns camelot.component.albums
  (:require [camelot.component.nav :as nav]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]))

(def custom-formatter (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(defn problem-component
  "Render a list item for a validation problem."
  [problem owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "album-problem"} (:reason problem)))))

(defn album-component
  "Render a list of validation problems."
  [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (if (empty? (:problems data))
                 (dom/label nil "No problems found. Time to analyse!")
                 (apply dom/ul nil
                        (om/build-all problem-component (:problems data))))))))

(defn albums-component [albums owner]
  "Render a list of albums and their validation results."
  (reify
    om/IRender
    (render [this]
      (let [contents (remove #(= (type %) js/String) albums)]
        (dom/div nil
                 (dom/label nil (first albums))
                 (apply dom/div nil
                        (om/build-all album-component contents)))))))

(defn album-summary-component [albums owner]
  "Render a summary of album validation results."
  (reify
    om/IRender
    (render [_]
      (let [pass-rate (/ (->> albums
                              (vals)
                              (map :problems)
                              (filter empty?)
                              (count))
                         (count albums))
            inty 200
            col-bias (* pass-rate inty)
            fmt "Folders passing validation: %.1f%%"
            colour (goog.string/format "rgb(%d, %d, 50)"
                                       (- inty col-bias) col-bias)]
        (dom/div #js {:className "album-validation-summary"}
                 (dom/label #js {:style #js {:color colour}}
                            (goog.string/format fmt (* pass-rate 100))))))))

(defn album-view-component [app owner]
  "Render an album validation summary."
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:onClick nav/settings-hide!}
               (dom/div nil (om/build album-summary-component (:albums app)))
               (dom/h3 nil "Problems Identified")
               (apply dom/div nil (om/build-all albums-component
                                                (:albums app)))))))

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
