(ns camelot.component.footer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn footer-component
  "Render the page footer."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [version (str "Version " (-> data :application :version))]
        (dom/div nil
                 (dom/img #js {:src "images/logo.png" :className "title"})
                 (dom/label #js {:className "version"} version))))))
