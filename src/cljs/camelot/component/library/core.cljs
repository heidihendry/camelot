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
            [camelot.component.library.identify :as identify]
            [camelot.component.library.collection :as collection]
            [camelot.translation.core :as tr]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [camelot.nav :as nav]))

(defn key-handler
  [e]
  (cond
    ;; >
    (and (= (.-keyCode e) 190) (.-shiftKey e))
    (do (.click (.getElementById js/document "next-page"))
        (nav/analytics-event "library-key" ">")
        (.preventDefault e))

    ;; <
    (and (= (.-keyCode e) 188) (.-shiftKey e))
    (do (.click (.getElementById js/document "prev-page"))
        (nav/analytics-event "library-key" "<")
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
        survey-id (aget opts "survey")
        reload (aget opts "reload")]
    (when reload
      (util/load-library data search))))

(defn delete-media!
  [data ids]
  (rest/delete-x "/media"
                 {:data {:media-ids ids}}
                 #(do
                    (om/update! data :show-delete-media-prompt false)
                    (om/update! data :selected-media-id nil)
                    (om/update! data :anchor-media-id nil)
                    (util/delete-with-ids! data ids)
                    (om/update! data :deferred-hydrate true))))

(defn delete-sightings!
  [data media-ids]
  (rest/delete-x "/sightings/media"
                 {:data {:media-ids media-ids}}
                 #(do
                    (om/update! data :show-delete-sightings-prompt false)
                    (dorun (map util/delete-sightings-from-media-with-id! media-ids)))))

(defn library-view-component
  "Render a collection of library."
  [data owner {:keys [restricted-mode]}]
  (reify
    om/IDidMount
    (did-mount [_]
      (when restricted-mode
        (om/update! (state/app-state-cursor) :restricted-mode true))
      (om/update! data [:library :search] {:last-search-terms ""
                                           :terms ""
                                           :survey-id (get-in (state/app-state-cursor)
                                                              [:selected-survey :survey-id :value])})
      (om/update! data [:library :search :page] 1)
      (om/update! data [:library :survey-id] (get-in (state/app-state-cursor) [:selected-survey :survey-id :value]))
      (om/update! data [:library :search :show-select-count] 0)
      (om/update! data [:library :identification] {:quantity 1})
      (rest/get-x "/surveys"
                  (fn [resp]
                    (om/update! (get data :library) :surveys (:body resp))))

      (let [sid (get-in (state/app-state-cursor)
                        [:selected-survey :survey-id :value])]
        (if sid
          (do
            (util/load-taxonomies (:library data) sid)
            (util/load-trap-stations (:library data) sid)
            (when-not (:restricted-mode @data)
              (util/load-library (:library data) (str "survey-id:" sid))))
          (do
            (util/load-taxonomies (:library data))
            (util/load-trap-stations (:library data))
            (when-not (:restricted-mode @data)
              (util/load-library (:library data)))))))
    om/IRender
    (render [_]
      (let [lib (:library data)]
        (if (or (not (nil? (get-in lib [:search :ordered-ids])))
                (:restricted-mode @data))
          (dom/div #js {:className (str "library" (if restricted-mode " restricted-mode" ""))
                        :onKeyDown key-handler
                        :tabIndex 0}
                   (if restricted-mode
                     (set! (.-tincan js/window) (partial tincan-listener lib))
                     (dom/div nil
                       (om/build search/search-component lib)
                       (om/build identify/identify-component lib)))
                   (dom/div #js {:className "media-control-panel"}
                            (let [media-ids (map :media-id (util/all-media-selected lib))]
                              (dom/div nil
                                       (om/build cutil/prompt-component lib
                                                 {:opts {:active-key :show-delete-media-prompt
                                                         :title (tr/translate ::delete-media-title)
                                                         :body (dom/div nil
                                                                        (dom/p #js {:className "delete-media-question"}
                                                                               (tr/translate ::delete-media-question))
                                                                        (dom/p nil (tr/translate ::media-select-count
                                                                                                 (count media-ids))))
                                                         :actions (dom/div #js {:className "button-container"}
                                                                           (dom/button #js {:className "btn btn-default"
                                                                                            :ref "action-first"
                                                                                            :onClick #(om/update! lib :show-delete-media-prompt false)}
                                                                                       (tr/translate :words/cancel))
                                                                           (dom/button #js {:className "btn btn-primary"
                                                                                            :ref "action-first"
                                                                                            :onClick #(delete-media! lib media-ids)}
                                                                                       (tr/translate :words/delete)))}})
                                       (om/build cutil/prompt-component lib
                                                 {:opts {:active-key :show-delete-sightings-prompt
                                                         :title (tr/translate ::delete-sightings-title)
                                                         :body (dom/div nil
                                                                        (dom/p #js {:className "delete-sightings-question"}
                                                                               (tr/translate ::delete-sightings-question))
                                                                        (dom/p nil (tr/translate ::sighting-select-count
                                                                                                 (count (mapcat :sightings (util/all-media-selected lib))))))
                                                         :actions (dom/div #js {:className "button-container"}
                                                                           (dom/button #js {:className "btn btn-default"
                                                                                            :ref "action-first"
                                                                                            :onClick #(om/update! lib :show-delete-sightings-prompt false)}
                                                                                       (tr/translate :words/cancel))
                                                                           (dom/button #js {:className "btn btn-primary"
                                                                                            :ref "action-first"
                                                                                            :onClick #(delete-sightings! lib media-ids)}
                                                                                       (tr/translate :words/delete)))}}))))
                   (dom/div #js {:className "library-body"}
                            (when (get-in lib [:search-results])
                              (om/build collection/media-collection-component lib))
                            (om/build preview/media-control-panel-component lib)))
          (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"})))))))
