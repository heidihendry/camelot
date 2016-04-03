(ns camelot.component.footer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn footer-component [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/img #js {:src "images/logo.png" :className "title"})
               (dom/label #js {:className "version"} (str "Version " (-> data :application :version)))
               (dom/label #js {:className "developer"}
                          (dom/a #js {:href "http://www.bitpattern.com.au" :target "_blank"} "Bit Pattern")
                          " © 2016 ")))))
