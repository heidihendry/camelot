(ns camelot.handler.settings
  (:require [camelot.config :as c]
            [camelot.model.photo :as mp]))

(declare flatten-nested-metadata)

(defn flatten-metadata-structure
  [md]
  (into [] (reduce #(concat %1 (if (= (type %2) clojure.lang.Keyword)
                                 [[%2]]
                               (flatten-nested-metadata %2)))
                   []
                md)))

(defn flatten-nested-metadata
  [[k md]]
  (into [] (map #(into [] (flatten [k (if (= (type %) clojure.lang.Keyword)
                                        [%]
                                        (flatten-nested-metadata %))])) md)))

(def metadata-paths (flatten-metadata-structure mp/metadata-structure))

(defn path-description
  [state path]
  (let [translate #((:translate state) %)]
    (->> path
         (map name)
         (clojure.string/join ".")
         (str "metadata/")
         (keyword)
         (translate))))

(defn get-metadata
  [state]
  (map #(hash-map :data %
                  :description (path-description state %))
       metadata-paths))

(defn settings-schema
  [state]
  {:config mp/config-schema
   :metadata (get-metadata state)})
