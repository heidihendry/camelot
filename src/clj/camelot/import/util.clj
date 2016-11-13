(ns camelot.import.util
  (:require
   [clojure.string :as str]
   [camelot.translation.core :as tr]))

(defn path-description
  "Return a translation for a given path"
  [state path]
  (->> path
       (map name)
       (str/join ".")
       (str "metadata/")
       (keyword)
       (tr/translate (:config state))))
