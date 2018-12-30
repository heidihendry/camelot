(ns camelot.backup.core
  (:require
   [camelot.util.maintenance :as maintenance]
   [camelot.util.state :as state]
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
  {:config (rel-file-seq (state/lookup-path state :config))
   :media (rel-file-seq (state/lookup-path state :media))
   :filestore-base (rel-file-seq (state/lookup-path state :filestore-base))
   :database (maintenance/backup state)})

(defn download
  [state filename]
  nil)
