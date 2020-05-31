(ns camelot.backup.core
  (:require
   [camelot.util.maintenance :as maintenance]
   [camelot.state.config :as config]
   [camelot.state.datasets :as datasets]
   [camelot.util.file :as file]
   [clojure.java.io :as io]))

(defn- rel-file-seq
  [path]
  (let [cpath (file/canonical-path path)]
    (->> cpath
         io/file
         file-seq
         (filter file/file?)
         (map #(subs (file/canonical-path %) (inc (count cpath)))))))

(defn manifest
  [state]
  {:config (rel-file-seq (config/lookup-path (:config state) :config))
   :media (rel-file-seq (datasets/lookup-path (:datasets state) :media))
   :filestore-base (rel-file-seq (datasets/lookup-path (:datasets state) :filestore-base))
   :database (maintenance/backup state)})

(defn download
  [state filename]
  nil)
