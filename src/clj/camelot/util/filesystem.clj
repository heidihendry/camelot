(ns camelot.util.filesystem
  (:require
   [clojure.java.io :as io]
   [camelot.util.file :as file]
   [clojure.string :as str])
  (:import
   (org.apache.commons.lang3 SystemUtils)
   (java.io IOException)))

(defn- replace-unsafe-chars
  [filename]
  (str/replace filename #"(?i)[^a-z0-9 .-_]+" "-"))

(defn checked-datadir
  "Check the custom datadir, and return the canonicalised path.
  throws an IO Exception if unsuitable."
  [dir]
  (let [f (io/file dir)]
    (when-not (file/exists? f)
      (throw (IOException. (str "File does not exist: " (file/get-path f)))))
    (when-not (file/directory? f)
      (throw (IOException. (str "File is not a directory: " (file/get-path f)))))
    (when (or (not (file/readable? f)) (not (file/writable? f)))
      (throw (IOException. (str "Permission denied: " (file/get-path f)))))
    (file/get-path f)))

(defn filestore-survey-directory
  "Return the path to the survey's filestore directory."
  [state survey-id]
  (io/file (get-in state [:config :path :filestore]) (str survey-id)))

(defn filestore-file-path
  "Return the path to a file in the survey's filestore directory."
  [state survey-id filename]
  (let [parent (filestore-survey-directory state survey-id)
        fs (str (file/get-path parent) SystemUtils/FILE_SEPARATOR
                (replace-unsafe-chars filename))]
    (when-not (file/exists? parent)
      (file/mkdirs parent))
    (checked-datadir parent)
    fs))
