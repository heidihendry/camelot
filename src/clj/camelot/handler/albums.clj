(ns camelot.handler.albums
  (:require [camelot.processing.dirtree :as r]
            [camelot.processing.album :as a]
            [camelot.processing.settings :refer [gen-state]]
            [camelot.processing.rename-photo :as rp]
            [camelot.util.java-file :as jf]))

(defn read-albums
  "Read photo directories and return metadata structured as albums."
  [state dir]
  (let [fdir (clojure.java.io/file dir)]
    (cond
      (nil? dir) ((:translate state) :problems/root-path-missing)
      (not (and (jf/exists? fdir) (jf/readable? fdir)))
      ((:translate state) :problems/root-path-not-found)
      :else
      (->> dir
           (r/read-tree state)
           (a/album-set state)))))
