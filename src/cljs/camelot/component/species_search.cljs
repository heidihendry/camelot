(ns camelot.component.species-search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [camelot.rest :as rest]
            [clojure.string :as str]
            [cljs.core.async :refer [<! chan >!]]
            [goog.string :as gstr]
            [om.core :as om]
            [om.dom :as dom]))

(def db-whitelist
  #{"Systema Dipterorum"
    "ITIS Global: The Integrated Taxonomic Information System"
    "ITIS Regional: The Integrated Taxonomic Information System"
    "WSC: World Spider Catalog"
    "FishBase"})

(defn get-classification
  [rank classifications]
  (gstr/unescapeEntities (or (get (first (filter #(= (get % "rank") rank) classifications))
                                  "name")
                             "")))

(defn process-result
  [res]
  (let [[genus species] (str/split (gstr/unescapeEntities (or (get res "name") ""))
                                   #" ")]
    {:id (get res "id")
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
    (rest/get-x "/species/search" {:query-params {"search" (-> e
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
                                      (lookup-species (:result-chan state) %)))}
                (dom/input #js {:type "text"
                                :name "search"
                                :placeholder "Scientific Name..."
                                :className "field-input species-search-input"
                                :onChange #(om/set-state! owner :search (.. % -target -value))
                                :value (:search state)})
                (dom/button #js {:type "submit"
                                 :name "submit"
                                 :className "btn btn-default"}
                            "Search")))))

(defn search-result-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/tr #js {:onClick #(go (>! (:select-chan state) data))}
              (dom/td nil (:genus data))
              (dom/td nil (:species data))
              (dom/td nil (dom/button #js {:className "btn btn-default"} "Add"))))))

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
                   (dom/table nil
                              (dom/thead nil
                                         (dom/tr #js {:className "table-heading"}
                                                 (dom/th nil "Genus")
                                                 (dom/th nil "Species")
                                                 (dom/th nil "")))
                              (dom/tbody #js {:className "selectable"}
                                         (om/build-all search-result-component
                                                       (:search-results data)
                                                       {:state state}))))
          (dom/span nil ))))))

(defn species-search-component
  [data owner]
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
                    (om/update! data :search-results (:results v))))
                (om/update! data :selection v))
              (recur))))))
    om/IRenderState
    (render-state [this state]
      (prn (:search-results data))
      (dom/div #js {:className "species-search"}
               (dom/label #js {:className "field-label"} "Search Species")
               (om/build search-input-component data
                         {:init-state {:result-chan (:result-chan state)}})
               (om/build search-result-list-component data
                         {:init-state {:select-chan (:select-chan state)}})))))
