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
            [clojure.string :as str]
            [camelot.nav :as nav]))

(defn print-key
  [e]
  (cond
    ;; crtl+p (frow wasd)
    (and (= (.-keyCode e) 78) (.-ctrlKey e))
    (do (.click (.getElementById js/document "next-page"))
        (nav/analytics-event "library-key" "C-p")
        (.preventDefault e))

    ;; ctrl+right
    (and (= (.-keyCode e) 39) (.-ctrlKey e))
    (do (.click (.getElementById js/document "next-page"))
        (nav/analytics-event "library-key" "C-<right>")
        (.preventDefault e))

    ;; ctrl+n (frow wasd)
    (and (= (.-keyCode e) 80) (.-ctrlKey e))
    (do (.click (.getElementById js/document "prev-page"))
        (nav/analytics-event "library-key" "C-n")
        (.preventDefault e))

    ;; ctrl+left
    (and (= (.-keyCode e) 37) (.-ctrlKey e))
    (do (.click (.getElementById js/document "prev-page"))
        (nav/analytics-event "library-key" "C-<left>")
        (.preventDefault e))

    ;; ctrl+f
    (and (= (.-keyCode e) 70) (.-ctrlKey e))
    (do (.focus (.getElementById js/document "filter"))
        (nav/analytics-event "library-key" "C-f")
        (.preventDefault e))

    ;; alt+f
    (and (= (.-keyCode e) 70) (.-altKey e))
    (do (.click (.getElementById js/document "apply-filter"))
        (nav/analytics-event "library-key" "M-f")
        (.preventDefault e))

    ;; ctrl+m
    (and (>= (.-keyCode e) 77) (.-ctrlKey e))
    (do (.focus (.getElementById js/document "media-collection-container"))
        (nav/analytics-event "library-key" "C-m"))

    ;; ctrl+i
    (and (= (.-keyCode e) 73) (.-ctrlKey e))
    (do (.click (.getElementById js/document "identify-selected"))
        (nav/analytics-event "library-key" "C-i"))

    ;; ctrl+d
    (and (= (.-keyCode e) 68) (.-ctrlKey e))
    (do (.click (.getElementById js/document "details-panel-toggle"))
        (nav/analytics-event "library-key" "C-d")
        (.preventDefault e))

    ;; ctrl+g
    (and (= (.-keyCode e) 71) (.-ctrlKey e))
    (do (.click (.getElementById js/document "media-flag"))
        (nav/analytics-event "library-key" "C-g")
        (.preventDefault e))

    ;; ctrl+h
    (and (= (.-keyCode e) 72) (.-ctrlKey e))
    (do (.click (.getElementById js/document "media-processed"))
        (nav/analytics-event "library-key" "C-h")
        (.preventDefault e))

    ;; ctrl+a
    (and (= (.-keyCode e) 65) (.-ctrlKey e))
    (do (.click (.getElementById js/document "select-all"))
        (nav/analytics-event "library-key" "C-a")
        (.preventDefault e))))

(defn library-view-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! (get-in data [:library :search]) :page 1)
      (om/update! (:library data) :survey-id (get-in (state/app-state-cursor) [:selected-survey :id]))
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

      (let [sid (get-in (state/app-state-cursor) [:selected-survey :id])]
        (if sid
          (do
            (util/load-trap-stations sid)
            (util/load-library sid))
          (do
            (util/load-trap-stations)
            (util/load-library)))))
    om/IRender
    (render [_]
      (let [lib (:library data)]
        (if (get-in lib [:search :results])
          (dom/div #js {:className "library"
                        :onKeyDown print-key
                        :tabIndex 0}
                   (om/build search/search-component lib)
                   (when (get-in lib [:search :matches])
                     (om/build collection/media-collection-component lib))
                   (om/build preview/media-control-panel-component lib))
          (dom/div nil ""))))))
