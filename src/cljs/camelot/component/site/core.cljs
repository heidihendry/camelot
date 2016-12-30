(ns camelot.component.site.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.component.util :as util]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.component.site.manage :as manage]
            [camelot.util.cursorise :as cursorise]
            [camelot.translation.core :as tr]
            [cljs.core.async :refer [<! chan >!]]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn delete
  "Delete the site and trigger a removal event."
  [state data event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (js/confirm (tr/translate ::confirm-delete))
    (rest/delete-x (str "/sites/" (:site-id data))
                   #(go (>! (:chan state) {:event :delete
                                           :data data})))))

(defn add-success-handler
  [data resp]
  (om/transact! data :list #(conj % (cursorise/decursorise (:body resp))))
  (om/update! data :new-site-name nil))

(defn add-site-handler
  [data]
  (rest/post-x "/sites"
               {:data {:site-name (:new-site-name data)}}
               (partial add-success-handler data))
  (nav/analytics-event "org-site" "create-click"))

(defn validate-proposed-site
  [data]
  (not (or (nil? (:new-site-name data))
           (let [site (-> data :new-site-name str/trim str/lower-case)]
             (or (empty? site)
                 (some #(= site (-> % str/trim str/lower-case))
                       (map :site-name (:list data))))))))

(defn add-site-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [is-valid (validate-proposed-site data)]
        (dom/form #js {:className "field-input-form"
                       :onSubmit #(.preventDefault %)}
                  (dom/input #js {:className "field-input"
                                  :placeholder (tr/translate ::new-site-name)
                                  :value (get-in data [:new-site-name])
                                  :onChange #(om/update! data :new-site-name
                                                         (.. % -target -value))})
                  (dom/input #js {:type "submit"
                                  :disabled (if is-valid "" "disabled")
                                  :title (when-not is-valid
                                           (tr/translate ::validation-duplicate-site))
                                  :className "btn btn-primary input-field-submit"
                                  :onClick #(add-site-handler data)
                                  :value (tr/translate :words/add)}))))))

(defn site-list-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item detailed dynamic"
                    :onClick #(nav/nav! (str "/site/" (:site-id data)))}
               (dom/div #js {:className "pull-right fa fa-times remove top-corner"
                             :onClick (partial delete state data)})
               (dom/span #js {:className "status pull-right"}
                         (:site-city data))
               (dom/span #js {:className "menu-item-title"}
                         (:site-name data))
               (dom/span #js {:className "menu-item-description"}
                         (when-not (empty? (:site-sublocation data))
                           (dom/span nil
                                     (dom/label nil (tr/translate :site/site-sublocation.label) ":")
                                     " "
                                     (:site-sublocation data) ", "))
                         (when-not (empty? (:site-state-province data))
                           (dom/span nil
                                     (dom/label nil
                                                (tr/translate :site/site-state-province.label) ":")
                                     " "
                                     (:site-state-province data))))
               (dom/div #js {:className "menu-item-description"}
                         (:site-notes data))))))

(defn manage-view
  [data owner {:keys [site-id]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! data :data nil)
      (om/update! data :list nil)
      (rest/get-x (str "/sites/" site-id)
                  #(do (om/update! data :data (:body %))
                       (rest/get-x "/sites/"
                                   (fn [x]
                                     (let [others (filter (fn [v] (not= (get-in (:body %) [:site-name :value])
                                                                        (:site-name v))) (:body x))]
                                       (om/update! data :list others)))))))
    om/IRender
    (render [_]
      (if (nil? (:list data))
        (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"}))
        (om/build manage/manage-component data)))))

(defn site-menu-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! data :list nil))
    om/IDidMount
    (did-mount [_]
      (rest/get-resource "/sites"
                         #(om/update! data :list (:body %)))
      (let [ch (om/get-state owner :chan)]
        (go
          (loop []
            (let [r (<! ch)]
              (cond
                (= (:event r) :delete)
                (om/transact! data :list #(remove (fn [x] (= x (:data r))) %))))
            (recur)))))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! data :list nil))
    om/IRenderState
    (render-state [_ state]
      (if (:list data)
        (dom/div #js {:className "section"}
                 (dom/div nil
                          (dom/input #js {:className "field-input"
                                          :value (:filter data)
                                          :placeholder (tr/translate ::filter-sites)
                                          :onChange #(om/update! data :filter (.. % -target -value))}))
                 (dom/div #js {:className "simple-menu scroll"}
                          (let [filtered (filter #(if (or (nil? (:filter data)) (empty? (:filter data)))
                                                    true
                                                    (re-matches (re-pattern (str "(?i).*" (:filter data) ".*"))
                                                                (str (:site-name %) " "
                                                                     (:site-city %) " "
                                                                     (:site-sublocation %) " "
                                                                     (:site-state-province %) " "
                                                                     (:site-country %) " "
                                                                     (:site-notes %))))
                                                 (sort-by :site-name (:list data)))]
                            (if (empty? filtered)
                              (om/build util/blank-slate-component {}
                                        {:opts {:item-name (tr/translate ::item-name)
                                                :advice (tr/translate ::advice)}})
                              (om/build-all site-list-component filtered
                                            {:key :site-id
                                             :init-state state}))))
                 (dom/div #js {:className "sep"})
                 (om/build add-site-component data)
                 (dom/button #js {:className "btn btn-default"
                                  :onClick #(do (nav/nav! "/sites")
                                                (nav/analytics-event "org-site" "advanced-click"))}
                             (tr/translate :words/advanced)))
        (dom/div #js {:className "align-center"}
                   (dom/img #js {:className "spinner"
                                 :src "images/spinner.gif"
                                 :height "32"
                                 :width "32"}))))))
