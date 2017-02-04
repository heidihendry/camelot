(ns camelot.component.library.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [camelot.util.filter :as filter]
            [camelot.component.util :as cutil]
            [camelot.component.library.util :as util]
            [camelot.component.library.search :as search]
            [camelot.component.library.preview :as preview]
            [camelot.component.library.collection :as collection]
            [camelot.translation.core :as tr]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [camelot.nav :as nav]))

(defn key-handler
  [e]
  (cond
    ;; ctrl+right
    (and (= (.-keyCode e) 39) (.-ctrlKey e))
    (do (.click (.getElementById js/document "next-page"))
        (nav/analytics-event "library-key" "C-<right>")
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
    (and (= (.-keyCode e) 77) (.-ctrlKey e))
    (do (.focus (.getElementById js/document "media-collection-container"))
        (nav/analytics-event "library-key" "C-m")
        (.preventDefault e))

    ;; ctrl+i
    (and (= (.-keyCode e) 73) (.-ctrlKey e))
    (do (.click (.getElementById js/document "identify-selected"))
        (nav/analytics-event "library-key" "C-i")
        (.preventDefault e))

    ;; ctrl+d
    (and (= (.-keyCode e) 68) (.-ctrlKey e))
    (do (.click (.getElementById js/document "details-panel-toggle"))
        (nav/analytics-event "library-key" "C-d")
        (.preventDefault e))))

(defn tincan-listener
  [data opts]
  (let [search (aget opts "search")
        reload (aget opts "reload")]
    (when reload
      (util/load-library-search search 0))))

(defn delete-media!
  [data id]
  (rest/delete-x (str "/media/" id)
                 #(do
                    (om/update! data :show-delete-media-prompt false)
                    (util/delete-with-id! id)
                    (om/update! data :selected-media-id nil))))

(defn library-view-component
  "Render a collection of library."
  [data owner {:keys [restricted-mode]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data [:library :search] {}))
    om/IDidMount
    (did-mount [_]
      (when restricted-mode
        (om/update! (state/app-state-cursor) :restricted-mode true))
      (om/update! (get-in data [:library :search]) :page 1)
      (om/update! (:library data) :search-results {})
      (om/update! (:library data) :survey-id (get-in (state/app-state-cursor) [:selected-survey :survey-id :value]))
      (om/update! (get-in data [:library :search]) :show-select-count 0)
      (om/update! (get-in data [:library]) :identification {:quantity 1})
      (rest/get-x "/surveys"
                  (fn [resp]
                    (om/update! (get data :library) :surveys (:body resp))))

      (let [sid (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])]
        (if sid
          (do
            (util/load-taxonomies sid)
            (util/load-trap-stations sid)
            (util/load-library sid))
          (do
            (util/load-taxonomies)
            (util/load-trap-stations)
            (util/load-library)))))
    om/IRender
    (render [_]
      (let [lib (:library data)]
        (if (get-in lib [:search :results])
          (dom/div #js {:className (str "library" (if restricted-mode " restricted-mode" ""))
                        :onKeyDown key-handler
                        :tabIndex 0}
                   (if restricted-mode
                     (set! (.-tincan js/window) (partial tincan-listener lib))
                     (om/build search/search-component lib))
                   (dom/div #js {:className "media-control-panel"}
                            (om/build cutil/prompt-component lib
                                      {:opts {:active-key :show-delete-media-prompt
                                              :title (tr/translate ::delete-media-title)
                                              :body (dom/div nil
                                                             (dom/p #js {:className "delete-media-question"}
                                                                    (tr/translate ::delete-media-question)))
                                              :actions (dom/div #js {:className "button-container"}
                                                                (dom/button #js {:className "btn btn-default"
                                                                                 :ref "action-first"
                                                                                 :onClick #(om/update! lib :show-delete-media-prompt false)}
                                                                            (tr/translate :words/cancel))
                                                                (dom/button #js {:className "btn btn-primary"
                                                                                 :ref "action-first"
                                                                                 :onClick #(delete-media! lib (:selected-media-id lib))}
                                                                            (tr/translate :words/delete)))}}))
                   (dom/div #js {:className "library-body"}
                            (when (get-in lib [:search-results])
                              (om/build collection/media-collection-component lib))
                            (om/build preview/media-control-panel-component lib)))
          (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"})))))))
