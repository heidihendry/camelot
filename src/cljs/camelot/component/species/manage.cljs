(ns camelot.component.species.manage
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [camelot.nav :as nav]
            [camelot.state :as state]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.component.species-search :as search]
            [om.dom :as dom]
            [clojure.string :as str]
            [camelot.util.cursorise :as cursorise]
            [camelot.translation.core :as tr])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn remove-species
  [taxonomy-id cb]
  (rest/delete-x
   (str "/taxonomy/" taxonomy-id "/survey/" (state/get-survey-id))
   cb))

(defn species-row-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/tr #js {:onClick #(go (>! (:rm-chan state) data))}
              (dom/td nil (:taxonomy-genus data))
              (dom/td nil (:taxonomy-species data))
              (dom/td nil (dom/button #js {:className "btn btn-default"}
                                      (tr/translate :words/remove)))))))

(defn survey-species-list
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:rm-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [chan (om/get-state owner :rm-chan)]
        (go
          (loop []
            (let [r (<! chan)]
              (remove-species (:taxonomy-id r)
                              (fn [x]
                                (do
                                  (om/transact! data :species #(into #{} (disj % r)))
                                  (nav/analytics-event "species-manage" "species-remove-click"))))
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (if (seq (:species data))
        (dom/div #js {:className "survey-species"}
                 (dom/table nil
                            (dom/thead nil
                                       (dom/tr #js {:className "table-heading"}
                                               (dom/th nil (tr/translate :taxonomy/taxonomy-genus.label))
                                               (dom/th nil (tr/translate :taxonomy/taxonomy-species.label))
                                               (dom/th nil "")))
                            (dom/tbody #js {:className "selectable"}
                                       (om/build-all species-row-component
                                                     (sort-by :taxonomy-genus (sort-by :taxonomy-species
                                                                                       (map cursorise/decursorise (:species data))))
                                                     {:init-state state}))))
        (dom/div #js {:className "no-species-found"}
                 (dom/p nil
                        (tr/translate ::search-instructions)))))))

(defn validate-proposed-species
  [data]
  (and (not (nil? (:new-species-name data)))
       (= (count (str/split (:new-species-name data) #" ")) 2)))

(defn add-taxonomy-success-handler
  [data extch resp]
  (let [species (cursorise/decursorise (:body resp))]
    (om/update! data :new-species-name nil)
    (om/update! data :taxonomy-create-mode false)
    (go (>! extch {:type :new :taxonomy species}))))

(defn add-taxonomy-handler
  [data extch]
  (let [segments (str/split (:new-species-name data) #" ")]
    (rest/post-x "/taxonomy"
                 {:data (merge {:taxonomy-genus (first segments)
                                :taxonomy-species (second segments)
                                :taxonomy-common-name (str (first segments) " " (second segments))
                                :survey-id (state/get-survey-id)})}
                 (partial add-taxonomy-success-handler data extch)))
  (nav/analytics-event "library-id" "taxonomy-create"))

(defn add-taxonomy-component
  [data owner {:keys [extch]}]
  (reify
    om/IRender
    (render [_]
      (let [is-valid (validate-proposed-species data)]
        (dom/form #js {:className "field-input-form"
                       :onSubmit #(.preventDefault %)}
                  (dom/input #js {:className "field-input inline long-input"
                                  :autoFocus "autofocus"
                                  :placeholder (tr/translate ::new-species-name-placeholder)
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
                                             (tr/translate ::validation-duplicate-species))
                                    :className "btn btn-primary input-field-submit"
                                    :onClick #(add-taxonomy-handler data extch)
                                    :value (tr/translate :words/add)})))))))

(defn species-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:taxonomy-id data)}
                  (:taxonomy-label data)))))

(defn taxonomy-select-component
  [data owner {:keys [extch]}]
  (reify
    om/IRender
    (render [_]
      (let [defined-spps (into #{} (map :taxonomy-id (:species data)))
            addable-spps (into '()
                               (sort-by :taxonomy-label
                                        (filter #(not (some (fn [x]
                                                              (= (:taxonomy-id %) x)) defined-spps))
                                                (vals (:known-species data)))))]
        (dom/div nil
                 (dom/label #js {:className "field-label"}
                            (tr/translate ::new-or-existing))
                 (if (or (empty? (:known-species data)) (:taxonomy-create-mode data))
                   (om/build add-taxonomy-component data
                             {:opts {:extch extch}})
                   (dom/select #js {:className "field-input"
                                    :id "identify-species-select"
                                    :value (get data :selected-species)
                                    :onChange #(let [v (.. % -target -value)]
                                                 (if (= v "create")
                                                   (do
                                                     (om/update! data :taxonomy-create-mode true)
                                                     (.focus (om/get-node owner)))
                                                   (do
                                                     (om/update! data :selected-species nil)
                                                     (go (>! extch {:type :select :taxonomy-id (cljs.reader/read-string v)})))))}
                               (om/build-all species-option-component
                                             (cons {:taxonomy-id -1
                                                    :taxonomy-label (str (tr/translate :words/select) "...")}
                                                   (reverse (conj addable-spps
                                                                  {:taxonomy-id "create"
                                                                   :taxonomy-label (tr/translate ::new-species)})))
                                             {:key :taxonomy-id}))))))))

(defn expected-species-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section survey-details-pane"}
               (dom/label #js {:className "field-label"}
                          (tr/translate ::expected-species))
               (om/build survey-species-list data)
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick #(do (nav/analytics-event "species-manage" "done-click")
                                                       (nav/nav-up!))}
                                    (tr/translate :words/done)))))))

(defn species-search-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (when (:species-search data)
        (dom/div #js {:className "section"}
                 (om/build taxonomy-select-component data
                           {:opts {:extch (:extch state)}})
                 (dom/p #js {:className "concept-separator"}
                        (dom/label #js {:className "guide"}
                                   (dom/span #js {:className "decorator"} "~ ")
                                   " " (tr/translate :words/or) " "
                                   (dom/span #js {:className "decorator"} "~ ")))
                 (om/build search/species-search-component (:species-search data)
                           {:opts {:extch (:extch state)}}))))))

(defn add-searched-species
  [data taxonomy]
  (rest/post-x "/species/create"
               {:data {:survey-id (state/get-survey-id)
                       :species [(select-keys taxonomy [:species :genus :id])]}}
               (fn [x]
                 (do
                   (om/transact! data :species #(conj % {:taxonomy-id (:taxonomy-id (first (:body x)))
                                                         :taxonomy-label (str (:genus taxonomy) " " (:species taxonomy))
                                                         :taxonomy-common-name (str (:genus taxonomy) " " (:species taxonomy))
                                                         :taxonomy-species (:species taxonomy)
                                                         :taxonomy-genus (:genus taxonomy)}))))))

(defn add-species-to-survey
  [data taxonomy-id]
  (prn (get-in data [:known-species]))
  (if-let [t (get-in data [:known-species taxonomy-id])]
    (rest/post-x "/taxonomy"
                 {:data (assoc (select-keys t [:taxonomy-id :taxonomy-species :taxonomy-genus])
                               :survey-id (state/get-survey-id))}
                 (fn [resp]
                   (om/transact! data :species #(conj % (cursorise/decursorise (:body resp))))))))

(defn dispatch-addition-event
  [data v]
  (case (:type v)
    :search (add-searched-species data (:taxonomy v))
    :select (add-species-to-survey data (:taxonomy-id v))
    :new (om/transact! data :species #(conj % (:taxonomy v)))))

(defn manage-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (go
        (loop []
          (let [ch (om/get-state owner :chan)
                v (<! ch)]
            (dispatch-addition-event data v)
            (recur)))))
    om/IRenderState
    (render-state [_ state]
      (if (get data :known-species)
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (tr/translate ::intro)))
                 (dom/div nil
                          (dom/div #js {:className "section-container"}
                                   (om/build expected-species-component data))
                          (dom/div #js {:className "section-container"}
                                   (om/build species-search-component data
                                             {:init-state {:extch (:chan state)}}))))
        (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"}))))))
