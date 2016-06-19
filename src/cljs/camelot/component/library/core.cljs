(ns camelot.component.library.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [camelot.util.filter :as filter]
            [camelot.component.library.util :as util]
            [camelot.component.library.search :as search]
            [camelot.component.library.preview :as preview]
            [camelot.component.library.collection :as collection]
            [cljs.reader :as reader]
            [clojure.string :as str]))

(defn library-view-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! (get-in data [:library :search]) :page 1)
      (om/update! (get-in data [:library :search]) :show-select-count 0)
      (om/update! (get-in data [:library]) :identification {:quantity 1})
      (rest/get-x "/taxonomy"
                  (fn [resp]
                    (om/update! (get data :library) :species
                                (into {}
                                      (map #(hash-map (get % :taxonomy-id) %)
                                           (:body resp))))))
      (rest/get-x "/surveys"
                  (fn [resp]
                    (om/update! (get data :library) :surveys (:body resp))))

      (util/load-trap-stations)
      (util/load-library))
    om/IRender
    (render [_]
      (let [lib (:library data)]
        (if (get-in lib [:search :results])
          (dom/div #js {:className "library"}
                   (om/build search/search-component lib)
                   (when (get-in lib [:search :matches])
                     (om/build collection/media-collection-component lib))
                   (om/build preview/media-control-panel-component lib))
          (dom/div nil ""))))))
