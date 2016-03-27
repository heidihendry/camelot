(ns camelot.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-http.client :as http]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cognitect.transit :as transit]
            [cljs.core.async :refer [<!]])
  (:import [goog.date UtcDateTime]))

(def transit-date-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (.getTime v))
   (fn [v] (str (.getTime v)))))

(defn transit-date-reader
  [s]
  (UtcDateTime.fromTimestamp s))

(def transit-file-reader identity)

(enable-console-print!)

(defonce app-state (atom {:text "Hello Chestnut!"}))

(defn request
  [method href params f]
  (go
    (let [response (<! (method href
                               {:transit-params params
                                :transit-opts {:decoding-opts
                                               {:handlers {"m" transit-date-reader
                                                           "f" transit-file-reader}}
                                               :encoding-opts
                                               {:handlers {UtcDateTime transit-date-writer}}}}))]
      (f response))))

(def postreq (partial request http/post))

(def getreq (partial request http/get))

(getreq (str (-> js/window (aget "location") (aget "href")) "albums")
        {}
        #(reset! app-state {:albums (:body %)}))

(def custom-formatter (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(defn update-description [e owner {:keys [description]}]
  (om/set-state! owner :description (.. e -target -value)))

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
                                               :onBlur #(postreq "update-photo-metadata" {:file (om/value file) :metadata (om/value metadata)} (fn [x] prn x))})))))))

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
               (dom/h3 nil (first albums))
               (apply dom/div nil
                      (om/build-all album-component albums))))))

(defn root-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil (apply dom/div nil (om/build-all albums-component (:albums app)))))))

(om/root
 root-component
 app-state
 {:target (js/document.getElementById "app")})
