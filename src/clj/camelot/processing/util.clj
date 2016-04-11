(ns camelot.processing.util
  (:require [clojure.string :as str]))

(defn path-description
  "Return a translation for a given path"
  [state path]
  (let [translate #((:translate state) %)]
    (->> path
         (map name)
         (str/join ".")
         (str "metadata/")
         (keyword)
         (translate))))
