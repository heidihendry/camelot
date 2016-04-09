(ns camelot.component.nav
  (:require [camelot.nav :as nav]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn nav-item-component
  "Render a list item for an item in the navigation bar."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (if (= (:function data) "settings")
        (dom/li #js {:id "settings-nav" :className "icon-only"
                     :onClick #(nav/toggle-settings!)}
                (dom/a nil (dom/span #js {:className "fa fa-cogs fa-2x"})))
        (dom/li #js {:onClick #(nav/nav! (:url data))}
                (dom/a nil (:label data)))))))

(defn nav-component
  "Render navigation bar and contents."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/ul #js {:className "nav navbar-nav"}
             (om/build-all nav-item-component (:menu-items data))))))
