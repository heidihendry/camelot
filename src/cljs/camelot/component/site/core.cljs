(ns camelot.component.site.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.component.util :as util]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.component.site.manage :as manage]
            [camelot.util.cursorise :as cursorise]
            [camelot.translation.core :as tr]))

(defn add-success-handler
  [data resp]
  (prn resp)
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
  (not (some #(= (:new-site-name data) %)
             (map :site-name (:list data)))))

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
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed dynamic"
                    :onClick #(nav/nav! (str "/site/" (:site-id data)))}
               (dom/span #js {:className "status pull-right"}
                         (:site-city data))
               (dom/span #js {:className "menu-item-title"}
                         (:site-name data))
               (dom/span #js {:className "menu-item-description"}
                         (when-not (empty? (:site-sublocation data))
                           (dom/span nil
                                     (dom/label nil (tr/translate :concepts/sublocation) ":")
                                     " "
                                     (:site-sublocation data) ", "))
                         (when-not (empty? (:site-city data))
                           (dom/span nil
                                     (dom/label nil
                                                (tr/translate :concepts/state-province) ":")
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
      (when-not (nil? (:list data))
        (om/build manage/manage-component data)))))

(defn site-menu-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource "/sites"
                         #(om/update! data :list (:body %))))
    om/IRender
    (render [_]
      (when (:list data)
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
                                            {:key :site-id}))))
                 (dom/div #js {:className "sep"})
                 (om/build add-site-component data)
                 (dom/button #js {:className "btn btn-default"
                                  :onClick #(do (nav/nav! "/sites")
                                                (nav/analytics-event "org-site" "advanced-click"))}
                             (tr/translate :words/advanced)))))))


