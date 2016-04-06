(ns camelot.component.albums
  (:require [camelot.util :as util]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]))

(defn update-description [e owner {:keys [description]}]
  (om/set-state! owner :description (.. e -target -value)))

(def custom-formatter (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(defn photo-component [[file metadata] owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/h4 nil (clojure.string/replace file #"^.*/" ""))
               (dom/ul nil
                       (dom/li nil "Date: " (tf/unparse custom-formatter (:datetime metadata)))
                       (dom/li nil "Camera: " (:make (:camera metadata)) " - " (:model (:camera metadata)))
                       (dom/li nil "Filesize: " (quot (:filesize metadata) 1024) " kB")
                       (dom/li nil "Description: "
                               (dom/input #js {:type "text" :ref "photo-description" :value (:description metadata)
                                               :onChange #(om/update! metadata :description (.. % -target -value))
                                               :onBlur #(util/postreq "update-photo-metadata" {:file (om/value file) :metadata (om/value metadata)} (fn [x] prn x))})))))))

(defn problem-component
  [problem owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "album-problem"} (:reason problem)))))

(defn album-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (if (empty? (:problems data))
                 (dom/label nil "No problems found. Time to analyse!")
                 (apply dom/ul nil
                        (om/build-all problem-component (:problems data))))))))

(defn albums-component [albums owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/label nil (first albums))
               (apply dom/div nil
                      (om/build-all album-component (remove #(= (type %) js/String) albums)))))))

(defn album-summary-component [albums owner]
  (reify
    om/IRender
    (render [_]
      (let [pass-rate (/ (->> albums
                              (vals)
                              (map :problems)
                              (filter empty?)
                              (count))
                         (count albums))
            col-bias (* pass-rate 200)]
        (dom/div #js {:className "album-validation-summary"}
                 (dom/label #js {:style #js {:color (goog.string/format "rgb(%d, %d, 50)" (- 200 col-bias) col-bias)}}
                            (goog.string/format "Folders passing validation: %.1f%%"
                                                (* pass-rate 100))))))))

(defn album-view-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div nil (om/build album-summary-component (:albums app)))
               (dom/h3 nil "Problems Identified")
               (apply dom/div nil (om/build-all albums-component (:albums app)))))))
