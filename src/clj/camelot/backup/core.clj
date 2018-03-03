(ns camelot.backup.core
  (:require
   [camelot.util.maintenance :as maintenance]
   [camelot.util.file :as file]
   [clojure.java.io :as io]))

(defn rel-file-seq
  [path]
  (let [cpath (file/canonical-path (io/file path))]
    (->> cpath
         io/file
         file-seq
         (filter file/file?)
         (map #(subs (file/canonical-path %) (inc (count cpath)))))))

(defn manifest
  [state]
  (let [path (-> state :config :path)]
    {:config (rel-file-seq (:config path))
     :media (rel-file-seq (:media path))
     :filestore-base (rel-file-seq (:filestore-base path))
     :database (maintenance/backup state)}))

(defn download
  [state filename]
  nil)
