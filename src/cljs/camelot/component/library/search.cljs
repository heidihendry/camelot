(ns camelot.component.library.search
  (:require [om.dom :as dom]
            [om.core :as om]
            [camelot.util.filter :as filter]
            [camelot.component.library.util :as util]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [camelot.nav :as nav]
            [typeahead.core :as typeahead]
            [clojure.string :as str]
            [cljs.core.async :refer [<! chan >! timeout]]
            [camelot.util.cursorise :as cursorise]
            [camelot.translation.core :as tr])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn add-sighting
  []
  (let [spp (cljs.reader/read-string (get-in (state/library-state) [:identification :species]))
        qty (get-in (state/library-state) [:identification :quantity])
        lifestage (get-in (state/library-state) [:identification :lifestage])
        sex (get-in (state/library-state) [:identification :sex])
        selected (:selected-media-id (state/library-state))
        all-selected (util/all-media-selected)]
    (rest/put-x "/library/identify" {:data (merge {:identification
                                                   {:quantity qty
                                                    :lifestage lifestage
                                                    :sex sex
                                                    :species spp}}
                                                  {:media
                                                   (map :media-id all-selected)})}
                (fn [resp]
                  (dorun (map #(do (om/update! (second %)
                                               :sightings
                                               (conj (:sightings (second %))
                                                     {:taxonomy-id spp
                                                      :sighting-lifestage lifestage
                                                      :sighting-sex sex
                                                      :sighting-id (first %)
                                                      :sighting-quantity qty}))
                                   (om/update! (second %) :media-processed true))
                              (zipmap (:body resp) all-selected)))
                  (.focus (.getElementById js/document "media-collection-container"))
                  (om/update! (:identification (state/library-state)) :quantity 1)
                  (om/update! (:identification (state/library-state)) :species -1)
                  (om/update! (:search (state/library-state)) :identify-selected false)))))

(defn identify-selected-prompt
  []
  (if-let [el (.getElementById js/document "identify-species-select")]
    (.focus el))
  (om/transact! (:search (state/library-state)) :identify-selected not))

(defn submit-identification
  []
  (identify-selected-prompt)
  (add-sighting))

(defn survey-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:survey-id data)}
                  (:survey-name data)))))

(defn species-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:taxonomy-id data)}
                  (:taxonomy-label data)))))

(defn filter-button-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "fa fa-search btn search"
                       :title (tr/translate ::filter-button-title)
                       :id "apply-filter"
                       :onClick #(do (om/update! data :dirty-state true)
                                     (nav/analytics-event "library-search" "forced-refresh-click"))}))))

(defn select-media-collection-container
  [e]
  (if (and (= (.-keyCode e) 85) (.-ctrlKey e))
    (do (om/update! (:search (state/library-state)) :terms "")
        (.preventDefault e))
    (when (= (.-keyCode e) 13)
      (let [node (.getElementById js/document "media-collection-container")]
        (.focus node)))))

(defn completion-field
  [ctx]
  (let [field (get filter/field-keys ctx)]
    (if field
      (name field)
      (some (set filter/model-fields) (list ctx)))))

(def prefix-endpoints
  {"survey" "/surveys"
   "site" "/sites"
   "trap" "/trap-stations"
   "camera" "/cameras"
   "taxonomy" "/taxonomy"})

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

      (= ctx "sighting-sex")
      (go
        (->> ["M" "F" "unidentified"]
             basic-word-index
             (>! ch)))

      (= ctx "sighting-lifestage")
      (go
        (->> ["adult" "juvenile" "unidentified"]
             basic-word-index
             (>! ch)))

      (or (nil? cf) (nil? ep)) nil

      :else
      (rest/get-x (str ep)
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

(defn filter-input-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [rf #(filter (complement nil?) (mapv %1 %2))]
        (rest/get-x "/taxonomy"
                    #(om/update! data :taxonomy-completions
                                 {:species (rf :taxonomy-label (:body %))
                                  :common-names (rf :taxonomy-common-name (:body %))}))))
    om/IRender
    (render [_]
      (om/build typeahead/typeahead (typeahead/phrase-index
                                     (apply conj (map #(hash-map :term %
                                                                 :props {:field true
                                                                         :completion-fn completions})
                                                      (apply conj (keys filter/field-keys)
                                                             filter/model-fields))
                                            (if (get-in data [:taxonomy-completions :species])
                                              (mapv typeahead/->basic-entry
                                                    (apply conj (get-in data [:taxonomy-completions :species])
                                                           (get-in data [:taxonomy-completions :common-names])))
                                              [])))
                {:opts {:input-config {:placeholder (tr/translate ::filter-placeholder)
                                       :className "field-input search"
                                       :title (tr/translate ::filter-title)
                                       :id "filter"
                                       :onChange (partial update-terms data)
                                       :value (get data :terms)
                                       :onKeyDown select-media-collection-container
                                       :disabled (if (get data :identify-selected)
                                                   "disabled" "")}
                        :multi-term true}}))))

(defn filter-survey-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/select #js {:className "survey-select field-input"
                       :title (tr/translate ::filter-survey-title)
                       :value (:survey-id data)
                       :onChange #(let [sid (cljs.reader/read-string (.. % -target -value))]
                                    (om/update! data :survey-id (.. % -target -value))
                                    (if (> sid -1)
                                      (do
                                        (util/load-taxonomies sid)
                                        (util/load-library sid)
                                        (util/load-trap-stations sid))
                                      (do
                                        (util/load-taxonomies)
                                        (util/load-library)
                                        (util/load-trap-stations)))
                                    (nav/analytics-event "library-search" "survey-select-change"))}
                  (om/build-all survey-option-component
                                (cons {:survey-id -1 :survey-name
                                       (tr/translate ::filter-survey-all-surveys)}
                                      (:surveys data))
                                {:key :survey-id})))))

(defn species-reference-filter
  [data spp]
  (if (string? spp)
    (str "species:"
         (:taxonomy-label
          (get (:species data)
               (cljs.reader/read-string spp))))
    ""))

(defn unidentified?
  [x]
  (or (nil? x) (= "unidentified" x)))

(defn maybe-unidentified-reference-filter
  [f x]
  (if (unidentified? x)
    ""
    (str f ":" x)))

(defn tincan-sender-wait
  [window opts]
  (go
    (loop []
      (if (.-tincan window)
        (.tincan window opts)
        (do
          (<! (timeout 100))
          (recur))))))

(defn build-reference-filter-string
  [data]
  (str (species-reference-filter data (get-in data [:identification :species]))
       " "
       (maybe-unidentified-reference-filter "sighting-sex"
                                            (get-in data [:identification :sex]))
       " "
       (maybe-unidentified-reference-filter "sighting-lifestage"
                                            (get-in data [:identification :lifestage]))
       " reference-quality:true"))

(defn tincan-sender
  [data reload {:keys [prevent-open]}]
  (let [features "menubar=no,location=no,resizable=no,scrollbars=yes,dependent=yes"]
    (if (and (:secondary-window data) (not (.-closed (:secondary-window data))))
      (do
        (.tincan (:secondary-window data)
                 #js {:search (build-reference-filter-string data)
                      :reload reload}))
      (when-not prevent-open
        (let [w (.open js/window (str (nav/get-token) "/restricted")
                       (str "Camelot | " (tr/translate ::reference-window-partial-title))
                       features)]
          (om/update! data :secondary-window w)
          (tincan-sender-wait w #js {:search (build-reference-filter-string data)
                                     :reload reload}))))))

(defn identification-panel-button-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/button #js {:className "btn btn-default"
                       :id "identify-selected"
                       :title (tr/translate ::identification-panel-button-title)
                       :onClick #(do (identify-selected-prompt)
                                     (nav/analytics-event "library-id" "open-identification-panel"))
                       :disabled (if (or (not (:has-selected state))
                                         (:identify-selected (:search data)))
                                   "disabled" "")}
                  (tr/translate ::identification-panel-button-text)))))

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
    om/IRender
    (render [_]
      (dom/span nil
               (dom/select #js {:className "trap-station-select field-input"
                                :value (:trap-station-id data)
                                :onChange #(let [sid (cljs.reader/read-string (.. % -target -value))]
                                             (om/update! (:search data) :trap-station-id sid)
                                             (om/update! (:search data) :page 1)
                                             (om/update! (:search data) :dirty-state true)
                                             (nav/analytics-event "library-search" "trap-station-select-change"))}
                           (om/build-all trap-station-option-component
                                         (cons {:trap-station-id -1
                                                :trap-station-name (tr/translate ::filter-trap-station-all-traps)}
                                               (:trap-stations data))
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
                               :value (get-in data [:search (:key state)])
                               :onChange #(do (om/update! (:search (state/library-state))
                                                          (:key state) (.. % -target -checked))
                                              (om/update! (:search data) :dirty-state true)
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

(defn search-bar-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [has-selected (first (filter (comp :selected util/find-with-id)
                                        (get-in data [:search :matches])))]
        (dom/div #js {:className "search-bar"}
                 (om/build filter-button-component (:search data))
                 (om/build filter-input-component (:search data))
                 (let [global-survey (get-in (state/app-state-cursor) [:selected-survey :survey-id :value])]
                   (do
                     (dom/span nil (str " " (tr/translate :words/in) " "))
                     (om/build filter-survey-component data)))
                 (om/build trap-station-select-component data)
                 (om/build subfilter-checkbox-component data
                           {:init-state {:key :unprocessed-only
                                         :label (tr/translate ::filter-unprocessed-label)}})
                 (dom/div #js {:className "pull-right action-container"}
                          (om/build media-flag-container-component data)
                          (dom/button #js {:className "btn btn-default"
                                           :onClick #(do
                                                       (let [sw (:secondary-window data)]
                                                         (when (and sw (not (.-closed sw)))
                                                           (.focus sw)))
                                                       (tincan-sender data true {}))
                                           :title (tr/translate ::reference-window-button-title)}
                                      (tr/translate ::reference-window-button-text))
                          (om/build identification-panel-button-component data
                                    {:state {:has-selected has-selected}})))))))

(defn validate-proposed-species
  [data]
  (and (not (nil? (:new-species-name data)))
       (= (count (str/split (:new-species-name data) #" ")) 2)))

(defn add-taxonomy-success-handler
  [data resp]
  (let [species (cursorise/decursorise (:body resp))]
    (om/transact! data :species #(conj % (hash-map (:taxonomy-id species) species)))
    (om/update! data :new-species-name nil)
    (om/update! (get-in data [:identification]) :species (str (:taxonomy-id species)))
    (om/update! data :taxonomy-create-mode false)))

(defn add-taxonomy-handler
  [data]
  (let [segments (str/split (:new-species-name data) #" ")]
    (rest/post-x "/taxonomy"
                 {:data (merge {:taxonomy-genus (first segments)
                                :taxonomy-species (second segments)
                                :taxonomy-common-name (tr/translate :words/na)}
                               (if (and (:survey-id data) (not= (:survey-id data) -1))
                                 {:survey-id (:survey-id data)}
                                 {}))}
                 (partial add-taxonomy-success-handler data)))
  (nav/analytics-event "library-id" "taxonomy-create"))

(defn add-taxonomy-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [is-valid (validate-proposed-species data)]
        (dom/form #js {:className "field-input-form inline"
                       :onSubmit #(.preventDefault %)}
                  (dom/input #js {:className "field-input inline long-input"
                                  :autoFocus "autofocus"
                                  :placeholder ::taxonomy-add-placeholder
                                  :value (get-in data [:new-species-name])
                                  :onChange #(om/update! data :new-species-name
                                                         (.. % -target -value))})
                  (if (empty? (:new-species-name data))
                    (dom/input #js {:type "submit"
                                    :className "btn btn-default input-field-submit"
                                    :onClick #(om/update! data :taxonomy-create-mode false)
                                    :value (tr/translate :words/cancel)})
                    (dom/input #js {:type "submit"
                                    :disabled (if is-valid "" "disabled")
                                    :title (when-not is-valid
                                             (tr/translate ::add-duplicate-species-error))
                                    :className "btn btn-primary input-field-submit"
                                    :onClick #(add-taxonomy-handler data)
                                    :value (tr/translate :words/add)})))))))

(defn taxonomy-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (if (or (empty? (:species data)) (:taxonomy-create-mode data))
        (om/build add-taxonomy-component data)
        (dom/select #js {:className "field-input auto-input"
                         :id "identify-species-select"
                         :value (get-in data [:identification :species])
                         :onChange #(let [v (.. % -target -value)]
                                      (if (= v "create")
                                        (do
                                          (om/update! data :taxonomy-create-mode true)
                                          (.focus (om/get-node owner)))
                                        (do
                                          (om/update! (:identification data) :species v)
                                          (om/update! (:identification data) :dirty-state true))))}
                    (om/build-all species-option-component
                                  (cons {:taxonomy-id -1
                                         :taxonomy-label (tr/translate :common/select)}
                                        (reverse (conj (into '()
                                                             (sort-by :taxonomy-label
                                                                      (vals (:species data))))
                                                       {:taxonomy-id "create"
                                                        :taxonomy-label (tr/translate ::add-new-species-label)})))
                                  {:key :taxonomy-id}))))))

(defn sighting-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:key data)} (:label data)))))

(defn sighting-lifestage-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/select #js {:className "field-input auto-input"
                       :value (get-in data [:identification :lifestage])
                       :onChange #(let [v (.. % -target -value)]
                                    (om/update! (:identification data) :lifestage v)
                                    (om/update! (:identification data) :dirty-state true))}
                  (om/build-all sighting-option-component
                                (list {:key "unidentified"
                                       :label (tr/translate :words/unidentified)}
                                      {:key "adult"
                                       :label (tr/translate :words/adult)}
                                      {:key "juvenile"
                                       :label (tr/translate :words/juvenile)})
                                {:key :key})))))

(defn sighting-sex-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/select #js {:className "field-input auto-input"
                       :value (get-in data [:identification :sex])
                       :onChange #(let [v (.. % -target -value)]
                                    (om/update! (:identification data) :sex v)
                                    (om/update! (:identification data) :dirty-state true))}
                  (om/build-all sighting-option-component
                                (list {:key "unidentified"
                                       :label (tr/translate :words/unidentified)}
                                      {:key "M"
                                       :label (tr/translate :words/male)}
                                      {:key "F"
                                       :label (tr/translate :words/female)})
                                {:key :key})))))

(defn identification-bar-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "identify-selected"
                                    (if (get-in data [:search :identify-selected])
                                      " show-prompt"
                                      ""
                                      ))}
               (dom/div nil
                        (dom/div #js {:className "field"}
                                 (dom/label nil (tr/translate :words/species))
                                 (om/build taxonomy-select-component data))
                        (dom/div #js {:className "field"}
                                 (dom/label nil (tr/translate :words/quantity))
                                 (dom/input #js {:type "number"
                                                 :className "field-input short-input"
                                                 :value (get-in data [:identification :quantity])
                                                 :onChange #(when (re-find #"^[0-9]+$"
                                                                           (get-in data [:identification :quantity]))
                                                              (om/update! (:identification data) :quantity
                                                                          (cljs.reader/read-string (.. % -target -value)))
                                                              (nav/analytics-event "library-id" "quantity-change"))}))
                        (dom/div #js {:className "field"}
                                 (dom/label nil (tr/translate :words/sex))
                                 (om/build sighting-sex-select-component data))
                        (dom/div #js {:className "field"}
                                 (dom/label nil (tr/translate :concepts/lifestage))
                                 (om/build sighting-lifestage-select-component data))
                        (dom/div #js {:className "field"}
                                 (dom/button #js {:className "btn btn-primary"
                                                  :disabled (when (or (not (and (get-in data [:identification :quantity])
                                                                                (get-in data [:identification :species])
                                                                                (> (get-in data [:identification :species]) -1)))
                                                                      (:taxonomy-create-mode data))
                                                              "disabled")
                                                  :onClick #(do (submit-identification)
                                                                (nav/analytics-event "library-id" "submit-identification"))}
                                             (tr/translate :words/submit))
                                 (dom/button #js {:className "btn btn-default"
                                                  :onClick #(do (om/update! (:search data) :identify-selected false)
                                                                (.focus (.getElementById js/document "media-collection-container"))
                                                                (nav/analytics-event "library-id" "cancel-identification"))}
                                             (tr/translate :words/cancel))))))))

(defn search-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! (:search data) :page 1)
      (om/update! (:search data) :matches (map :media-id (filter/only-matching nil data)))
      (om/update! (:search data) :terms nil))
    om/IRender
    (render [_]
      (when (-> data :search :dirty-state)
        (om/update! (:search data) :dirty-state false)
        (om/update! (:search data) :matches
                    (map :media-id (filter/only-matching (-> data :search :terms) data))))
      (when (-> data :identification :dirty-state)
        (om/update! (:identification data) :dirty-state false)
        (tincan-sender data false {:prevent-open true}))
      (dom/div #js {:className "search-container"}
               (om/build search-bar-component data)
               (om/build identification-bar-component data)))))
