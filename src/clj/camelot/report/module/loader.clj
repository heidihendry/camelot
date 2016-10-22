(ns camelot.report.module.loader
  (:require [camelot.util.java-file :as f]
            [camelot.util.config :as conf]
            [clojure.java.io :as io]
            [camelot.report.module.builtin.core])
  (:import [org.apache.commons.lang3 SystemUtils]))

(def report-dir-name "reports")
(def column-dir-name "columns")

(def clj-file-re #"(?i)\.clj$")

(defn- module-path
  [subdir]
  (format "%s%s%s%s%s" (conf/get-config-dir)
          SystemUtils/FILE_SEPARATOR
          "modules"
          SystemUtils/FILE_SEPARATOR
          subdir))

(defn- clj-file?
  "Predicate for whether a given file is a clojure file."
  [file]
  (or (and (f/file? file)
           (f/readable? file)
           (re-find clj-file-re (f/get-name file))
           true)
      false))

(defn load-modules-from-subdir
  [subdir]
  (let [dir (io/file (module-path subdir))
        modules (file-seq dir)]
    (when-not (f/exists? dir)
      (f/mkdirs dir))
    (doseq [file (filter clj-file? modules)]
      (try
        (load-file (f/get-path file))
        (catch Exception e
          (println "Error loading module: " file "\n"
                   (.getMessage e)))))))

(defn load-user-modules
  []
  (load-modules-from-subdir report-dir-name)
  (load-modules-from-subdir column-dir-name))
