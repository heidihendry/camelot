(ns camelot.report.module.loader
  (:require
   [camelot.util.file :as file]
   [camelot.app.state :as state]
   [camelot.report.module.builtin.core])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(def clj-file-re #"(?i)\.clj$")

(defn- module-path
  []
  (format "%s%s%s" (state/get-config-dir)
          SystemUtils/FILE_SEPARATOR
          "modules"))

(defn- clj-file?
  "Predicate for whether a given file is a clojure file."
  [file]
  (or (and (file/file? file)
           (file/readable? file)
           (re-find clj-file-re (file/get-name file))
           true)
      false))

(defn load-user-modules
  []
  (let [dir (file/->file (module-path))
        modules (file-seq dir)]
    (when-not (file/exists? dir)
      (file/mkdirs dir))
    (doseq [file (filter clj-file? modules)]
      (try
        (load-file (file/get-path file))
        (catch Exception e
          (println "Error loading module: " file "\n"
                   (.getMessage e)))))))
