(ns camelot.component.backup.core
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]))

(defn menu-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil "backup"))))
