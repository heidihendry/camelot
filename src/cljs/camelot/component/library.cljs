(ns camelot.component.library
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]))

(defn library-media-component
  "Render a single library item."
  [result owner]
  (reify
    om/IRender
    (render [_]
      (prn result)
      (dom/span nil
                (dom/img #js {:src (get-in result [:media-uri])
                              :width "100"
                              :height "100"
                             :className "library"})))))

(defn library-collection-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (apply dom/div
                      (om/build-all library-media-component
                                    (get-in data [:search :results])
                                    {:key :media-id}))))))

(defn library-view-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      ;; TODO For now we assume there's only 1 survey.
      (rest/get-x "/species"
                  (fn [resp] (om/update! (get data :library)
                                         :species
                                         (into {} (map #(hash-map (get-in % [:species-id :value]) %)
                                                       (:body resp))))))
      (rest/get-x "/library"
                  (fn [resp] (om/update! (get-in data [:library :search])
                                         :results (:body resp)))))
    om/IRender
    (render [_]
      (dom/div #js {:className "library"}
               (om/build library-collection-component (:library data))))))
