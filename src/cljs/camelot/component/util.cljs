(ns camelot.component.util
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.translation.core :as tr]))

(def escape-keycode 27)

(defn blank-slate-component
  [data owner {:keys [item-name advice notice]}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "blank-slate"}
               (dom/div #js {:className "large"}
                        (or notice
                            (tr/translate ::blank-notice-template item-name)))
               (dom/div #js {:className "advice"}
                        (or advice
                            (tr/translate ::use-button-below)))))))

(defn blank-slate-beta-component
  [data owner {:keys [item-name]}]
  (reify
    om/IRender
    (render [_]
      (om/build blank-slate-component {}
                {:opts {:item-name item-name
                        :advice (tr/translate ::use-advanced-menu)}}))))

(defn dialog-classname
  [data active-key]
  (if (get data active-key)
    "prompt-dialog"
    "prompt-dialog hide-above"))


(defn focus-subchild
  [owner ref pos]
  (let [children (-> (om/get-node owner ref) .-children (aget 0) .-children)
        focuser #(.focus (aget children %))]
    (case pos
      :first (focuser 0)
      :last (focuser (dec (.-length children)))
      :else nil)))

(defn prompt-component
  [data owner {:keys [form-opts active-key title body actions closable]}]
  (reify
    om/IInitState
    (init-state [_]
      {:focused false})
    om/IDidUpdate
    (did-update  [_ _ _]
      (let [focused (om/get-state owner :focused)]
        (when (and (get data active-key) (not focused))
          (if-let [cl (om/get-node owner "close-button")]
            (.focus cl))
          (om/set-state! owner :focused true))
        (when (and (not (get data active-key)) focused)
          (om/set-state! owner :focused false))))
    om/IRender
    (render [_]
      (dom/div #js {:className "prompt"}
               (when (get data active-key)
                 (dom/div #js {:className "blanket"}))
               (dom/div #js {:className "tabguard"
                             :tabIndex "0"
                             :onFocus #(focus-subchild owner "actions" :last)})
               (dom/div #js {:className (dialog-classname data active-key)
                             :onKeyDown #(when (= (.-keyCode %) escape-keycode)
                                           (om/update! data active-key false))
                             :ref "dialog"}
                        (when-not (= closable false)
                          (dom/button #js {:className "pull-right fa fa-times btn-flat"
                                           :ref "close-button"
                                           :onClick #(om/update! data active-key false)}))
                        (dom/form (clj->js (or form-opts {:onSubmit #(.preventDefault %)}))
                                  (dom/div #js {:className "prompt-title"}
                                           title)
                                  (dom/div #js {:className "prompt-body"}
                                           body)
                                  (dom/div #js {:className "prompt-actions"
                                                :ref "actions"}
                                           actions)))
               (dom/div #js {:className "tabguard"
                             :tabIndex "0"
                             :onFocus #(if-let [cl (om/get-node owner "close-button")]
                                         (.focus cl))})))))
