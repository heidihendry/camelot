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

(defn album-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (apply dom/div nil
                      (om/build-all photo-component (:photos data)))))))

(defn albums-component [albums owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/div nil
                        (dom/h3 nil (first albums))
                        (apply dom/div nil
                               (om/build-all album-component albums)))))))

(defn album-view-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil (apply dom/div nil (om/build-all albums-component (:albums app)))))))
