(ns camelot.component.survey.sighting-fields
  "Management of sighting fields."
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as str]
            [camelot.rest :as rest]
            [camelot.translation.core :as tr]
            [camelot.util.sighting-fields :as util.sf]
            [camelot.component.util :as cutil]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [cljs.core.async :refer [<! chan >!]]
            [goog.date :as date]
            [camelot.validation.validated-component :as vc]
            [camelot.util.cursorise :as cursorise]
            [camelot.util.data :as data])
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [camelot.macros.ui.validation :refer [with-validation]]))

(def sighting-field-input-keys
  "Minimal set of keys which a sighting field must possess."
  #{:sighting-field-key
    :sighting-field-label
    :sighting-field-datatype
    :sighting-field-options
    :sighting-field-default
    :sighting-field-required
    :sighting-field-affects-independence
    :sighting-field-ordering})

(def allowed-request-fields
  (into [:survey-id] sighting-field-input-keys))

(defn- datatype-has-options?
  [data]
  (get-in util.sf/datatypes [(keyword (get data :sighting-field-datatype)) :has-options]))

(defn- update-buffer
  "Set buffer based the selected sighting-field ID."
  [data sf-id]
  (let [sf (get-in data [::sighting-fields sf-id])
        vs (-> (data/require-keys sf sighting-field-input-keys)
               (update :sighting-field-datatype #(or % "text"))
               (update :sighting-field-required #(or % "false"))
               (update :sighting-field-affects-independence #(or % "false"))
               (update :sighting-field-ordering #(or % 20))
               (assoc ::field-key-edited (not (empty? (:sighting-field-key sf)))))]
    (om/update! data ::buffer vs)))

(defn survey-sighting-field-keys
  [data]
  (->> (::sighting-fields data)
       vals
       (filter #(= (:survey-id %) (state/get-survey-id)))
       (map :sighting-field-key)
       (into #{})))

(defn compatible-sighting-fields
  [data]
  (let [sf-keys (survey-sighting-field-keys data)]
    (->> (::sighting-fields data)
         vals
         (remove #(= (:survey-id %) (state/get-survey-id)))
         (remove #(sf-keys (:sighting-field-key %)))
         (map (fn [sf] [(:sighting-field-id sf)
                        (str (:sighting-field-label sf) " ("
                             (get-in data [::surveys (:survey-id sf) :survey-name]) ")")])))))

(defn select-option-component
  "Render a component for a select option."
  [[key value] owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value key} value))))

(defn copy-from-survey-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [fields (compatible-sighting-fields data)]
        (when (seq fields)
          (dom/div nil
                   (dom/label nil (tr/translate ::template)) ": "
                   (dom/select #js {:onChange #(om/update! data ::field-template-id
                                                           (let [v (.. % -target -value)]
                                                             (if v (cljs.reader/read-string v) v)))}
                               (om/build-all select-option-component
                                             (conj fields ["" (tr/translate ::default-template)])
                                             {:key-fn first}))))))))

(defn- select-sighting-field
  "Select the sighting field with the given ID."
  [data sf-id]
  (if (contains? (::sighting-fields data) sf-id)
    (update-buffer data sf-id)
    (om/update! data ::buffer nil))
  (if (or (= sf-id ::new)
          (contains? (::sighting-fields data) sf-id))
    (om/update! data ::selected-sighting-field-id sf-id)
    (om/update! data ::selected-sighting-field-id nil)))

(defn- copy-sighting-field
  "Select the sighting field with the given ID."
  [data sf-id]
  (prn sf-id)
  (if sf-id
    (update-buffer data sf-id)
    (om/update! data ::buffer nil))
  (om/update! data ::selected-sighting-field-id ::new))

(defn sighting-field-menu-item
  "Menu item representing a single sighting field."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [sid (get-in data [::item :sighting-field-id])]
        (dom/div #js {:className (str "menu-item" (if (= (get-in data [::context ::selected-sighting-field-id])
                                                         sid) " active" ""))
                      :onClick #(do
                                  (nav/analytics-event "sighting-fields" "survey-sighting-field-click")
                                  (select-sighting-field (::context data) sid))}
                 (dom/div #js {:className "pull-right fa fa-times remove top-corner"
                               :onClick #(do
                                           (.preventDefault %)
                                           (.stopPropagation %)
                                           (om/update! (::context data) ::delete-sighting-field-id sid)
                                           (om/update! (::context data) ::show-delete-sighting-field-prompt true))})
                 (get-in data [::item :sighting-field-label]))))))

(defn menu-component
  "Side menu for sighting fields within a survey."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [fields (->> (::sighting-fields data)
                        vals
                        (filter #(= (:survey-id %) (state/get-survey-id)))
                        (sort-by (juxt :sighting-field-ordering :sighting-field-label))
                        (map #(hash-map ::context data ::item %)))]
        (dom/div #js {:className "section"}
                 (dom/div #js {:className "simple-menu"}
                          (if (seq fields)
                            (om/build-all sighting-field-menu-item fields
                                          {:key-fn #(get-in % [::item :sighting-field-id])})
                            (om/build cutil/blank-slate-component {}
                                      {:opts {:notice (tr/translate ::help-text)}})))
                 (dom/div #js {:className "sep"})
                 (om/build copy-from-survey-component data)
                 (dom/button #js {:className "btn btn-primary"
                                  :onClick #(do
                                              (nav/analytics-event "sighting-fields" "new-field-click")
                                              (copy-sighting-field data (::field-template-id data)))}
                             (if (number? (::field-template-id data))
                               (tr/translate ::new-field-from-template)
                               (tr/translate ::new-field))))))))

(defn field-translation
  "Return the translation for the field. Suffix may be either '.label' or '.description'."
  [field suffix]
  (let [ns "camelot.component.survey.sighting-fields"]
    (tr/translate (keyword (str ns "/" (name field) suffix)))))

(defn submit-button-component
  "Submit the current form."
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/button #js {:className "btn btn-primary"
                       :disabled (if (::validated data) "" "disabled")
                       :onClick #(do
                                   (nav/analytics-event "sighting-fields" "submit-click")
                                   (go (>! (::chan state)
                                           {:event :submit
                                            :sighting-field-id (::selected-sighting-field-id data)
                                            :buffer (update (::buffer data) :sighting-field-datatype keyword)})))}
                  (if (= (::selected-sighting-field-id data) ::new)
                    (tr/translate :words/create)
                    (tr/translate :words/update))))))

(defn cancel-button-component
  "Cancel any changes."
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/button #js {:className "btn btn-default"
                       :onClick #(do
                                   (nav/analytics-event "sighting-fields" "cancel-click")
                                   (go (>! (::chan state) {:event :cancel})))}
                  (tr/translate :words/cancel)))))

(defn select-component
  "Render a component for selections, adding in any additional attributes specified by `attrs`.
Options for select are given by the `options` option."
  [data owner {:keys [field required attrs options]}]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className (str "field-label " (if required "required" ""))}
                          (tr/translate (field-translation field ".label")))
               (dom/select (clj->js (merge {:className "field-input"
                                            :required required
                                            :onChange #(om/update! data field
                                                                   (.. % -target -value))
                                            :value (get data field)
                                            :title (tr/translate (field-translation field ".description"))}
                                           (or attrs {})))
                           (om/build-all select-option-component options
                                         {:key-fn first}))))))

(defn build-text-input-component
  "Builds a component which executes the given function on change."
  [onchange]
  (fn [data owner {:keys [field required attrs]}]
    (reify
      om/IRender
      (render [_]
        (dom/div nil
                 (dom/label #js {:className (str "field-label " (if required "required" ""))}
                            (tr/translate (field-translation field ".label")))
                 (dom/input (clj->js (merge {:className "field-input"
                                             :required required
                                             :onChange (onchange data field)
                                             :value (get data field)
                                             :title (tr/translate (field-translation field ".description"))}
                                            (or attrs {})))))))))

(defn label-to-field-key
  [label]
  (-> (str/lower-case label)
      (str/replace #"'" "")
      (str/replace #"[^-0-9a-z]+" "-")
      (str/replace #"^-*" "")
      (str/replace #"-*$" "")))

(def text-input-component
  (build-text-input-component (fn [data field] #(om/update! data field (.. % -target -value)))))

(def field-label-input-component
  (build-text-input-component
   (fn [data field]
     (fn [evt]
       (let [v (.. evt -target -value)]
         (om/update! data field v)
         (when-not (::field-key-edited data)
           (om/update! data :sighting-field-key (label-to-field-key v))))))))

(def field-key-input-component
  (build-text-input-component
   (fn [data field]
     (fn [evt]
       (let [v (.. evt -target -value)]
         (om/update! data field v)
         (om/update! data ::field-key-edited (not (empty? v))))))))

(defn- string-list-item
  "Display and removal of a single list item."
  [{:keys [data field value]} owner]
  (reify
    om/IRender
    (render [_]
      (let [rm-fn #(om/transact! data field (fn [v] (remove #{value} v)))]
        (dom/span #js {:className "list-item"}
                  value
                  (dom/span #js {:className "list-item-delete fa fa-times"
                                 :onClick rm-fn}))))))

(defn- field-options-component
  "Component for managing lists of items"
  [data owner {:keys [field required attrs]}]
  (reify
    om/IInitState
    (init-state [_]
      {::text-value ""})
    om/IRenderState
    (render-state [this state]
      (when (datatype-has-options? data)
        (letfn [(additem! [] (do (let [v (str/trim (om/get-state owner ::text-value))]
                                   (when-not (some #{v} (get data field))
                                     (om/transact! data field #(conj % v))))
                                 (om/set-state! owner ::text-value "")))]
          (dom/label #js {:className (str "field-label " (if required "required" ""))}
                     (tr/translate (field-translation field ".label")))
          (dom/div #js {:className "list-input"}
                   (dom/div #js {:className "list-input-add-container"}
                            (dom/input #js {:type "text" :className "field-input" :placeholder (tr/translate ::add-option)
                                            :value (get state ::text-value)
                                            :required required
                                            :onKeyDown #(when (= (.-key %) "Enter") (additem!))
                                            :onChange #(om/set-state! owner ::text-value (.. % -target -value))})
                            (dom/button #js {:className "btn btn-primary"
                                             :onClick additem!}
                                        (tr/translate :words/add)))
                   (apply dom/div #js {:className "list-input-items"}
                          (om/build-all string-list-item
                                        (into [] (map #(hash-map :data data :field field :value %)
                                                      (sort (get data field))))
                                        {:key-fn identity}))))))))

(defn edit-component
  "Component for editing sighting field details."
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:result-vchan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [rvchan (om/get-state owner :result-vchan)]
        (om/set-state! owner :validation-chan
                       (vc/component-validator rvchan))
        (go-loop []
          (let [{:keys [validated]} (<! rvchan)]
            (om/update! data ::validated validated)
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (go
        (>! (om/get-state owner :result-vchan) {:command :unmount})))
    om/IRenderState
    (render-state [_ state]
      (if-let [buf (::buffer data)]
        (when (or (= (get-in data [::buffer :sighting-field-id])
                     (::selected-sighting-field-id data))
                  (= (::selected-sighting-field-id data) ::new))
          (dom/div #js {:className "section"}
                   (when (::selected-sighting-field-id data)
                     (dom/div nil
                              (with-validation (:validation-chan state) dom/div {}
                                (om/build field-label-input-component buf
                                          {:data-key :sighting-field-label
                                           :validators [(vc/required) (vc/max-length 255)]
                                           :params {:opts {:field :sighting-field-label
                                                           :required true}}})
                                (om/build field-key-input-component buf
                                          {:data-key :sighting-field-key
                                           :validators [(vc/required)
                                                        (vc/keyword-like)
                                                        (vc/unique (->> (::selected-sighting-field-id data)
                                                                        (dissoc (::sighting-fields data))
                                                                        vals
                                                                        (filter #(= (:survey-id %) (state/get-survey-id)))
                                                                        (map :sighting-field-key)
                                                                        (into #{})))]
                                           :params {:opts {:field :sighting-field-key
                                                           :required true}}})
                                (om/build select-component buf
                                          {:data-key :sighting-field-datatype
                                           :validators [(vc/required)]
                                           :params {:opts
                                                    {:field :sighting-field-datatype
                                                     :required true
                                                     :options
                                                     (letfn [(f [[k v]]
                                                               [(name k) (tr/translate (:translation-key v))])]
                                                       (sort (map f util.sf/datatypes)))}}})
                                (dom/div #js {:className "sighting-field-options"}
                                         (om/build field-options-component buf
                                                   {:data-key :sighting-field-options
                                                    :validators [(vc/required-if #(datatype-has-options? buf))]
                                                    :params {:opts {:field :sighting-field-options}}}))
                                (om/build select-component buf
                                          {:data-key :sighting-field-required
                                           :validators [(vc/required)]
                                           :params {:opts
                                                    {:field :sighting-field-required
                                                     :required true
                                                     :options (into [] {"true" (tr/translate :words/yes)
                                                                        "false" (tr/translate :words/no)})}}})
                                (om/build text-input-component buf
                                            {:data-key :sighting-field-default
                                             :validators []
                                             :params {:opts {:field :sighting-field-default}}})
                                (om/build select-component buf
                                          {:data-key :sighting-field-affects-independence
                                           :validators [(vc/required)]
                                           :params
                                           {:opts {:field :sighting-field-affects-independence
                                                   :required true
                                                   :options (into [] {"true" (tr/translate :words/yes)
                                                                      "false" (tr/translate :words/no)})}}})
                                (om/build text-input-component buf
                                          {:data-key :sighting-field-ordering
                                           :validators [(vc/required)]
                                           :params
                                           {:opts {:field :sighting-field-ordering
                                                   :required true
                                                   :attrs {:type "number"}}}}))
                              (dom/div #js {:className "button-container"}
                                       (om/build cancel-button-component data {:state state})
                                       (om/build submit-button-component data {:state state}))))))
        (do (update-buffer data nil)
            nil)))))

(defn assoc-sighting-field
  [data sighting-field]
  (om/transact! data ::sighting-fields #(assoc % (:sighting-field-id sighting-field) sighting-field)))

(defn manage-fields-component
  "Top-level component for managing a survey's sighting fields."
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {::chan (chan)})
    om/IWillMount
    (will-mount [_]
      (rest/get-x "/sighting-fields"
                  (fn [r] (om/update! data ::sighting-fields
                                      (data/key-by :sighting-field-id (:body r)))))
      (rest/get-x "/surveys"
                  (fn [r] (om/update! data ::surveys (data/key-by :survey-id (:body r)))))
      (update-buffer data nil)
      (let [ch (om/get-state owner ::chan)]
        (go-loop []
          (let [r (<! ch)]
            (condp = (:event r)
              :submit
              (let [v (select-keys (assoc (:buffer r) :survey-id (state/get-survey-id))
                                   allowed-request-fields)]
                (let [sf-id (:sighting-field-id r)]
                  (if (number? sf-id)
                    (rest/put-x (str "/sighting-fields/" sf-id) {:data v}
                                #(do (assoc-sighting-field data (cursorise/decursorise (:body %)))
                                     (select-sighting-field data nil)))
                    (rest/post-x "/sighting-fields" {:data v}
                                 #(do (assoc-sighting-field data (cursorise/decursorise (:body %)))
                                      (select-sighting-field data nil)
                                      (om/update! data ::field-template-id nil))))))

              :delete
              (let [sf-id (:sighting-field-id r)]
                (rest/delete-x (str "/sighting-fields/" sf-id)
                               #(do (om/transact! data ::sighting-fields (fn [m] (dissoc m sf-id)))
                                    (select-sighting-field data (::selected-sighting-field-id data))))
                (om/update! data ::delete-sighting-field-id nil)
                (om/update! data ::show-delete-sighting-field-prompt false))

              :cancel
              (select-sighting-field data nil))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! data ::sighting-fields nil)
      (om/update! data ::selected-sighting-field-id nil)
      (om/update! data ::surveys nil)
      (om/update! data ::buffer nil)
      (om/update! data ::field-template-id nil))
    om/IRenderState
    (render-state [_ state]
      (if (and (::surveys data) (::sighting-fields data))
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "back-button-container"}
                          (dom/button #js {:className "btn btn-default back"
                                           :onClick #(nav/nav-up! 1)}
                                      (dom/span #js {:className "fa fa-mail-reply"})
                                      " " (tr/translate :words/back)))
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (tr/translate ::page-title)))
                 (dom/div nil
                      (dom/div #js {:className "section-container"}
                               (om/build menu-component data))
                      (dom/div #js {:className "section-container"}
                               (om/build edit-component data {:state state})))
                 (om/build cutil/prompt-component data
                           {:opts {:active-key ::show-delete-sighting-field-prompt
                                   :title (get-in data [::sighting-fields (::delete-sighting-field-id data) :sighting-field-label])
                                   :body (dom/div nil
                                                  (dom/p #js {:className "delete-question"}
                                                         (tr/translate ::delete-question)))
                                   :actions (dom/div #js {:className "button-container"}
                                                     (dom/button #js {:className "btn btn-default"
                                                                      :ref "action-first"
                                                                      :onClick #(om/update! data ::show-delete-sighting-field-prompt false)}
                                                                 (tr/translate :words/cancel))
                                                     (dom/button #js {:className "btn btn-primary"
                                                                      :ref "action-first"
                                                                      :onClick #(do
                                                                                  (nav/analytics-event "sighting-fields" "delete-click")
                                                                                  (go (>! (::chan state)
                                                                                          {:event :delete
                                                                                           :sighting-field-id (::delete-sighting-field-id data)})))}
                                                                 (tr/translate :words/delete)))}}))
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))))))
