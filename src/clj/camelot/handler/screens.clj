(ns camelot.handler.screens
  (:require [camelot.model.screens :as screens]))

(defn all-screens
  "Return settings, menu and configuration definitions"
  [state]
  (screens/smith state))
