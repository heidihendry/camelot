(ns camelot.component.species-search
  (:require [camelot.rest :as rest]
            [camelot.nav :as nav]
            [clojure.string :as str]
            [cljs.core.async :refer [<! chan >!]]
            [goog.string :as gstr]
            [om.core :as om]
            [om.dom :as dom]
            [camelot.state :as state]
            [camelot.translation.core :as tr])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def db-whitelist
  #{"Systema Dipterorum"
    "ITIS Global: The Integrated Taxonomic Information System"
    "ITIS Regional: The Integrated Taxonomic Information System"
    "WSC: World Spider Catalog"
    "FishBase"})

(defn get-classification
  [rank classifications]
  (gstr/unescapeEntities
   (or (get (first (filter #(= (get % "rank") rank) classifications))
            "name")
       "")))

(defn process-result
  [res]
  (let [[genus species] (str/split (gstr/unescapeEntities (or (get res "name") ""))
                                   #" ")]
    {:id (get res "id")
     :citation (get res (gstr/unescapeEntities "bibliographic_citation"))
     :species species
     :genus genus}))

(defn process-all-results
  [raw]
  (vec (apply sorted-set-by #(let [g (compare (:genus %1) (:genus %2))]
                         (if (= g 0)
                           (compare (:species %1) (:species %2))
                           g))
                      (filter #(and (:genus %) (:species %))
                              (map process-result
                                   (filter #(and (contains? db-whitelist (get % "source_database"))
                                                 (= (get % "name_status") "accepted name"))
                                           (get raw "results")))))))

(defn lookup-species
  [chan e]
  (go
    (>! chan {:busy true})
    (rest/get-x-raw "/species/search" {:query-params {"search" (-> e
                                                               .-target
                                                               array-seq
                                                               first
                                                               .-value)}}
                #(go
                   (>! chan {:busy false
                             :results (process-all-results (:body %))})))))

(defn search-input-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:search ""})
    om/IRenderState
    (render-state [_ state]
      (dom/form #js {:onSubmit #(do (.preventDefault %)
                                    (when-not (:busy data)
                                      (lookup-species (:result-chan state) %)))
                     :className "field-input-form"}
                (dom/input #js {:type "text"
                                :name "search"
                                :placeholder (tr/translate ::scientific-name)
                                :className "field-input inline long-input"
                                :onChange #(om/set-state! owner :search (.. % -target -value))
                                :value (:search state)})
                (dom/input #js {:type "submit"
                                :name "submit"
                                :disabled (if (or (nil? (:search state))
                                                  (< (count (.trim (:search state))) 5))
                                            "disabled"
                                            "")
                                :className "btn btn-default input-field-submit"
                                :value (tr/translate :words/search)})))))

(defn truncate
  [s nc]
  (if (> (count s) nc)
    (str (subs s 0 nc) "...")
    s))

(defn search-result-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/tr #js {:onClick #(go (>! (:select-chan state) {:type :selection
                                                           :data data}))}
              (dom/td nil (:genus data))
              (dom/td nil (:species data))
              (dom/td #js {:colSpan "2"
                           :className "action-column"}
                      (dom/p #js {:className "button-container"}
                             (dom/button #js {:className "btn btn-default"
                                              :onClick #(do
                                                          (.preventDefault %)
                                                          (.stopPropagation %)
                                                          (go (>! (:select-chan state) {:type :citation
                                                                                        :data (:citation data)})))}
                                         (tr/translate :words/citation))
                             (dom/button #js {:className "btn btn-primary"}
                                         (tr/translate :words/add))))))))

(defn search-result-list-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (if (:busy data)
        (dom/img #js {:className "spinner"
                      :src "images/spinner.gif"
                      :height "32"
                      :width "32"})
        (if (seq (:search-results data))
          (dom/div #js {:className "scroll"}
                   (dom/table #js {:className "species-search-table"}
                              (dom/thead nil
                                         (dom/tr #js {:className "table-heading"}
                                                 (dom/th nil (tr/translate :taxonomy/taxonomy-genus.label))
                                                 (dom/th nil (tr/translate :taxonomy/taxonomy-species.label))
                                                 (dom/th nil "")))
                              (dom/tbody #js {:className "selectable"}
                                         (om/build-all search-result-component
                                                       (:search-results data)
                                                       {:state state}))))
          (dom/span nil ))))))

(defn citation-modal
  [data owner]
  (reify
    om/IRender
    (render [_]
      (when (:citation data)
        (dom/div #js {:className "citation-modal"}
                 (dom/h3 nil (tr/translate :words/citation))
                 (dom/p #js {:className "citation-font"} (:citation data))
                 (dom/button #js {:onClick #(om/update! data :citation nil)
                                  :className "btn btn-primary hide-citation"}
                             (tr/translate :words/hide)))))))

(defn species-search-component
  [data owner {:keys [extch]}]
  (reify
    om/IInitState
    (init-state [_]
      {:results []
       :result-chan (chan)
       :select-chan (chan)})
    om/IDidMount
    (did-mount [_]
      (let [res (om/get-state owner :result-chan)
            sel (om/get-state owner :select-chan)]
        (go
          (loop []
            (let [[v port] (alts! [res sel])]
              (if (= port res)
                (do
                  (when-not (nil? (:busy v))
                    (om/update! data :busy (:busy v)))
                  (when (:results v)
                    (om/update! data :search-results (:results v))
                    (nav/analytics-event "species-search" "search")))
                (do
                  (if (= (:type v) :citation)
                    (do
                      (om/update! data :citation (:data v))
                      (nav/analytics-event "species-search" "view-citation"))
                    (do
                      (if extch
                        (>! extch {:type :search :taxonomy (:data v)})
                        (om/update! data :selection (:data v)))
                      (nav/analytics-event "species-search" "add-species")))))
              (recur))))))
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "species-search"}
               (om/build citation-modal data)
               (dom/label #js {:className "field-label"}
                          (tr/translate ::search-species))
               (om/build search-input-component data
                         {:init-state {:result-chan (:result-chan state)}})
               (om/build search-result-list-component data
                         {:init-state {:select-chan (:select-chan state)}})))))
