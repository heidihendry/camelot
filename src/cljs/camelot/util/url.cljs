(ns camelot.util.url
  (:require
   [cemerick.url :refer [url]]))

(defn window-href
  []
  (-> js/window
      (aget "location")
      (aget "href")
      url))

(defn hide-settings?
  []
  (-> (window-href) :query :hidesettings))
