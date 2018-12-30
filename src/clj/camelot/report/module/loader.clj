(ns camelot.report.module.loader
  (:require
   [camelot.util.file :as file]
   [camelot.util.state :as state]
   [camelot.report.module.builtin.core])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(def clj-file-re #"(?i)\.clj$")

(defn module-path
  "Return the module path."
  [state]
  (file/->file (state/lookup-path state :config) "modules"))

(defn- clj-file?
  "Predicate for whether a given file is a clojure file."
  [file]
  (or (and (file/file? file)
           (file/readable? file)
           (re-find clj-file-re (file/get-name file))
           true)
      false))

(defn- load-module-file
  "Load a module file, catching any exception which may be raised."
  [file]
  (try
    (load-file (file/get-path file))
    (catch Exception e
      (println "Error loading module: " file "\n"
               (.getMessage e)))))

(defn load-user-modules
  "Load all modules in the module-path."
  [state]
  (let [dir (module-path state)
        modules (file-seq dir)]
    (when-not (file/exists? dir)
      (file/mkdirs dir))
    (doall (map load-module-file (filter clj-file? modules)))))
