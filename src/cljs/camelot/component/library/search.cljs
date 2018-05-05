(ns camelot.component.library.search
  (:require [om.dom :as dom]
            [om.core :as om]
            [camelot.component.library.util :as util]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [camelot.nav :as nav]
            [camelot.util.search :as search]
            [camelot.util.sighting-fields :as sighting-fields]
            [typeahead.core :as typeahead]
            [clojure.string :as str]
            [cljs.core.async :refer [<! chan >! timeout sliding-buffer]]
            [camelot.util.cursorise :as cursorise]
            [camelot.translation.core :as tr]
            [camelot.util.feature :as feature]
            [camelot.component.library.identify :as identify])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn survey-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:survey-id data)}
                  (:survey-name data)))))

(defn filter-button-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/button #js {:className (str "fa fa-search btn search" (if (not= (:terms data) (:last-search-terms data))
                                                                   " search-dirty" ""))
                       :title (tr/translate ::filter-button-title)
                       :id "apply-filter"
                       :disabled (if (:inprogress data) "disabled" "")
                       :onClick #(do (go (>! (:search-chan state) {:search (deref data)}))
                                     (nav/analytics-event "library-search" "forced-refresh-click"))}))))

(defn select-media-collection-container
  [state data e]
  (.persist e)
  (when (= (.-keyCode e) 13)
    (let [node (.getElementById js/document "media-collection-container")]
      (go (>! (:search-chan state)
              {:search (assoc (deref data) :terms (.. e -target -value))}))
      (.focus node))))

(defn completion-field
  [ctx]
  (let [field (get search/field-keys (keyword ctx))]
    (cond
      (re-find #"^field-" ctx) ctx
      field (name field)
      :else (some #{ctx} search/model-fields))))

(def prefix-endpoints
  {"survey" "/surveys"
   "site" "/sites"
   "trap" "/trap-stations"
   "camera" "/cameras"
   "taxonomy" "/taxonomy"
   "field" "/sighting-field-values"})

(defn basic-word-index
  [xs]
  (->> xs
       (mapv typeahead/->basic-entry)
       typeahead/word-index))

(defn completions
  [ctx ch]
  (let [cf (completion-field ctx)
        ep (get prefix-endpoints (first (str/split cf #"-")))]
    (cond
      (some #(= ctx %) '("flagged" "processed" "testfire" "reference-quality"))
      (go
        (->> ["true" "false"]
             basic-word-index
             (>! ch)))

      (or (nil? cf) (nil? ep)) nil

      :else
      (rest/get-x ep
                  #(go (>! ch (->> (:body %)
                                   (mapv (keyword cf))
                                   (filter (complement nil?))
                                   (mapv typeahead/->basic-entry)
                                   typeahead/phrase-index)))))))

(defn update-terms
  [data terms]
  (om/update! data :terms terms)
  (om/update! data :page 1)
  (om/update! data :dirty-state true))

(defn sighting-field-to-field-user-key
  [sighting-field]
  (name (sighting-fields/user-key sighting-field)))

(defn filter-input-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:typeahead-index (typeahead/phrase-index
                         (apply conj (map #(hash-map :term %
                                                     :props {:field true
                                                             :completion-fn completions})
                                          (apply conj
                                                 (apply conj (map name (keys search/field-keys))
                                                        search/model-fields)
                                                 (map sighting-field-to-field-user-key
                                                      (apply concat (vals (:sighting-fields data))))))
                                (if (get-in data [:search :taxonomy-completions :species])
                                  (mapv typeahead/->basic-entry
                                        (apply conj (get-in data [:search :taxonomy-completions :species])
                                               (get-in data [:search :taxonomy-completions :common-names])))
                                  [])))})
    om/IWillMount
    (will-mount [_]
      (let [rf #(filter (complement nil?) (mapv %1 %2))]
        (rest/get-x "/taxonomy"
                    #(om/update! data :taxonomy-completions
                                 {:species (rf :taxonomy-label (:body %))
                                  :common-names (rf :taxonomy-common-name (:body %))}))))
    om/IRenderState
    (render-state [_ state]
      (om/build typeahead/typeahead (:typeahead-index state)
                {:opts {:input-config {:placeholder (tr/translate ::filter-placeholder)
                                       :className "field-input search"
                                       :title (tr/translate ::filter-title)
                                       :id "filter"
                                       :onChange #(om/update! data [:search :terms] %)
                                       :onKeyDown #(select-media-collection-container state (:search data) %)}
                        :multi-term true}
                 :state {:disabled (:inprogress data)}}))))

(defn filter-survey-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/select #js {:className "survey-select field-input"
                       :title (tr/translate ::filter-survey-title)
                       :value (get-in data [:search :survey-id] "")
                       :disabled (if (get-in data [:search :inprogress]) "disabled" "")
                       :onChange #(let [sid (cljs.reader/read-string (.. % -target -value))]
                                    (om/update! data [:search :survey-id] sid)
                                    (om/update! data [:search :trap-station-id] -1)
                                    (if (> sid -1)
                                      (do
                                        (util/load-taxonomies data sid)
                                        (util/load-trap-stations data sid)
                                        (go (>! (:search-chan state) {:search (assoc (:search @data)
                                                                                     :survey-id sid
                                                                                     :trap-station-id nil)})))
                                      (do
                                        (util/load-taxonomies data)
                                        (util/load-trap-stations data)
                                        (go (>! (:search-chan state) {:search (assoc (:search @data)
                                                                                     :survey-id sid
                                                                                     :trap-station-id nil)}))))
                                    (nav/analytics-event "library-search" "survey-select-change"))}
                  (om/build-all survey-option-component
                                (cons {:survey-id -1 :survey-name
                                       (tr/translate ::filter-survey-all-surveys)}
                                      (sort-by :survey-name (:surveys data)))
                                {:key :survey-id})))))

(defn species-reference-filter
  [data spp]
  (if (string? spp)
    (let [tax (:taxonomy-label (get (:species data) (cljs.reader/read-string spp)))]
      (if tax
        (str "species:\"" tax "\"")
        ""))
    ""))

(defn tincan-sender-wait
  [window opts]
  (go
    (loop []
      (if (.-tincan window)
        (.tincan window opts)
        (do
          (<! (timeout 100))
          (recur))))))

(defn- sighting-field-reference-filters
  [data]
  (let [identification (:identification data)
        sf-list (map #(vector (:sighting-field-id %) (sighting-field-to-field-user-key %))
                     (apply concat (vals (:sighting-fields data))))]
    (mapcat (fn [[id key]]
              (let [value (get-in identification [:sighting-fields id])]
                (if (empty? (str value))
                  []
                  [(str key ":" value)])))
            sf-list)))

(defn build-reference-filter-string
  [data]
  (str (species-reference-filter data (get-in data [:identification :species]))
       " "
       (str/join " " (sighting-field-reference-filters data))
       " reference-quality:true"))

(defn tincan-sender
  [data reload {:keys [prevent-open]}]
  (let [features "menubar=no,location=no,resizable=no,scrollbars=yes,dependent=yes"]
    (if (and (:secondary-window data) (not (.-closed (:secondary-window data))))
      (do
        (.tincan (:secondary-window data)
                 #js {:search (build-reference-filter-string data)
                      :survey (let [sid (get-in data [:search :survey-id])]
                                (if (and sid (not= sid -1))
                                  sid
                                  nil))
                      :reload reload}))
      (when-not prevent-open
        (let [w (.open js/window (str (nav/get-token) "/restricted")
                       (str "Camelot | " (tr/translate ::reference-window-partial-title))
                       features)]
          (om/update! data :secondary-window w)
          (tincan-sender-wait w #js {:search (build-reference-filter-string data)
                                     :survey (let [sid (get-in data [:search :survey-id])]
                                               (if (and sid (not= sid -1))
                                                 sid
                                                 nil))
                                     :reload reload}))))))

(defn trap-station-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:trap-station-id data)}
                  (:trap-station-name data)))))

(defn trap-station-select-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/span nil
               (dom/select #js {:className "trap-station-select field-input"
                                :value (get-in data [:search :trap-station-id] "")
                                :disabled (if (get-in data [:search :inprogress]) "disabled" "")
                                :onChange #(let [sid (cljs.reader/read-string (.. % -target -value))]
                                             (om/update! (:search data) :trap-station-id sid)
                                             (go (>! (:search-chan state) {:search (assoc (deref (:search data))
                                                                                          :trap-station-id sid)}))
                                             (nav/analytics-event "library-search" "trap-station-select-change"))}
                           (om/build-all trap-station-option-component
                                         (cons {:trap-station-id -1
                                                :trap-station-name (tr/translate ::filter-trap-station-all-traps)}
                                               (sort-by :trap-station-name (:trap-stations data)))
                                         {:key :trap-station-id}))))))

(defn subfilter-checkbox-component
  [data owner]
  (reify
    om/IRenderState
    (render-state
      [_ state]
      (dom/div #js {:className "checkbox-container"}
                (dom/label nil (:label state))
                (dom/input #js {:type "checkbox"
                                :value (get-in data [:search (:key state)] "")
                                :disabled (if (get-in data [:search :inprogress]) "disabled" "")
                                :onChange #(do (om/update! (:search (state/library-state))
                                                           (:key state) (.. % -target -checked))
                                               (go (>! (:search-chan state) {:search (assoc (deref (:search data))
                                                                                            (:key state) (.. % -target -checked))}))
                                               (nav/analytics-event "library-search"
                                                                    (str (str/lower-case (:label state)) "-checkbox-change")))
                                :className "field-input"})))))

(defn media-flag-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [selected (util/all-media-selected)
            flag-enabled (and (seq selected) (every? (:key state) selected))]
        (when (seq selected)
          (dom/span #js {:className ((:classFn state) flag-enabled)
                         :title (:title state)
                         :id (:id state)
                         :onClick #(do ((:fn state) (not flag-enabled))
                                       (nav/analytics-event "library-search" (str (:id state) "-toggled")))}))))))

(defn media-flag-container-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/span nil
                (om/build media-flag-component data
                          {:init-state {:title (tr/translate ::flag-media-title)
                                        :key :media-attention-needed
                                        :fn util/set-attention-needed
                                        :id "media-flag"
                                        :classFn #(str "fa fa-2x fa-flag flag"
                                                       (if % " red" ""))}})
                (om/build media-flag-component data
                          {:init-state {:title (tr/translate ::media-cameracheck-title)
                                        :key :media-cameracheck
                                        :fn util/set-cameracheck
                                        :id "media-cameracheck"
                                        :classFn #(str "fa fa-2x fa-user testfire"
                                                       (if % " lightblue" ""))}})
                (om/build media-flag-component data
                          {:init-state {:title (tr/translate ::media-processed-title)
                                        :key :media-processed
                                        :fn util/set-processed
                                        :id "media-processed"
                                        :classFn #(str "fa fa-2x fa-check processed"
                                                       (if % " green" ""))}})
                (om/build media-flag-component data
                          {:init-state {:title (tr/translate ::media-reference-quality-title)
                                        :key :media-reference-quality
                                        :fn util/set-reference-quality
                                        :id "media-reference-quality"
                                        :classFn #(str "fa fa-2x fa-trophy reference-quality"
                                                       (if % " yellow " ""))}})))))

(defn search-bar-options
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/span #js {:className "search-option-container"}
                (om/build filter-input-component data {:init-state state})
                (om/build filter-button-component (:search data) {:init-state state})
                (let [global-survey (get-in (state/app-state-cursor)
                                            [:selected-survey :survey-id :value])]
                  (do
                    (dom/span nil (str " " (tr/translate :words/in) " "))
                    (om/build filter-survey-component data {:init-state state})))
                (om/build trap-station-select-component data {:init-state state})
                (om/build subfilter-checkbox-component data
                          {:init-state {:key :unprocessed-only
                                        :label (tr/translate ::filter-unprocessed-label)
                                        :search-chan (:search-chan state)}})))))

(defn search-bar-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [has-selected (first (util/all-media-selected))]
        (dom/div #js {:className "search-bar"}
                 (om/build search-bar-options data {:init-state state})
                 (dom/div #js {:className "pull-right action-container"}
                          (om/build media-flag-container-component data)
                          (dom/button #js {:className "btn btn-default"
                                           :onClick #(do
                                                       (let [sw (:secondary-window data)]
                                                         (when (and sw (not (.-closed sw)))
                                                           (.focus sw)))
                                                       (tincan-sender data true {}))
                                           :title (tr/translate ::reference-window-button-title)}
                                      (tr/translate ::reference-window-button-text))))))))

(defn search
  [data search]
  (let [survey-id (or (:survey-id search) (get-in @data [:search :survey-id]))]
    (let [terms (search/append-subfilters (:terms search) (assoc (deref (:search data))
                                                                 :survey-id survey-id))]
      (util/load-library data terms)
      (om/update! data [:search :last-search-terms] (:terms search)))))

(defn search-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:search-chan (chan (sliding-buffer 1))})
    om/IWillMount
    (will-mount [_]
      (om/update! (:search data) :mode :search)
      (om/update! (:search data) :page 1)
      (go
        (loop []
          (let [ch (om/get-state owner :search-chan)
                r (<! ch)]
            (om/update! (:search data) :dirty-state false)
            (search data (:search r))
            (om/update! (:search data) :page 1)
            (recur)))))
    om/IRenderState
    (render-state [_ state]
      (when (-> data :search :dirty-state)
        (go (>! (:search-chan state) {:search (deref (:search data))})))
      (when (-> data :identification :dirty-state)
        (om/update! (:identification data) :dirty-state false)
        (tincan-sender data true {:prevent-open true}))
      (dom/div #js {:className "search-container"}
               (om/build search-bar-component data {:init-state state})))))
