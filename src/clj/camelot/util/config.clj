(ns camelot.util.config
  (:require [camelot.translation.core :as tr :refer :all]
            [camelot.util.java-file :as jf]
            [clj-time
             [coerce :as tc]
             [core :as t]]
            [clojure
             [edn :as edn]
             [pprint :as pp]]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [camelot.util.java-file :as jf]
            [clojure.string :as str])
  (:import [org.apache.commons.lang3 SystemUtils]
           [java.lang RuntimeException]
           [java.io IOException]))

(def default-config
  "Return the default configuration."
  {:erroneous-infrared-threshold 0.2
   :infrared-iso-value-threshold 999
   :language :en
   :root-path nil
   :night-end-hour 5
   :night-start-hour 21
   :project-start (tc/to-long (t/now))
   :project-end (tc/to-long (t/now))
   :sighting-independence-minutes-threshold 20
   :surveyed-species []
   :required-fields [[:headline] [:artist] [:phase] [:copyright]
                     [:location :gps-longitude] [:location :gps-latitude]
                     [:datetime] [:filename]]})

(def os (System/getProperty "os.name"))
(def db-name "Database")
(def media-directory-name "Media")
(def filestore-directory-name "FileStore")

(def config-filename "config.clj")

(defn- config-dir
  "Return the path to the root-level configuration directory."
  [dir]
  (format "%s%scamelot" dir SystemUtils/FILE_SEPARATOR))

(defn- config-path
  "Return the full path where the configuration file is stored."
  [dir]
  (format "%s%s%s" (config-dir dir) SystemUtils/FILE_SEPARATOR config-filename))

(defn get-config-location
  [loc-fn]
  (cond
    SystemUtils/IS_OS_WINDOWS (loc-fn (env :appdata))
    SystemUtils/IS_OS_LINUX (loc-fn (str (env :home) "/.config"))
    SystemUtils/IS_OS_MAC_OSX (loc-fn (str (env :home) "/Library/Preferences"))
    :else (loc-fn (str (env :pwd) ".camelot"))))

(defn get-config-file
  "Return the OS-specific path to the configuration file."
  []
  (get-config-location config-path))

(defn get-config-dir
  "Return the OS-specific path to the configuration directory."
  []
  (get-config-location config-dir))

(defn checked-datadir
  "Check the custom datadir, and return the canonicalised path.
  throws an IO Exception if unsuitable."
  [dir]
  (let [f (io/file dir)]
    (when (not (jf/exists? f))
      (throw (IOException. (str "File does not exist: " (jf/get-path f)))))
    (when (not (jf/directory? f))
      (throw (IOException. (str "File is not a directory: " (jf/get-path f)))))
    (when (or (not (jf/readable? f)) (not (jf/writable? f)))
      (throw (IOException. (str "Permission denied: " (jf/get-path f)))))
    (jf/get-path f)))

(defn- db-path
  "Return the full path where the database is stored."
  [dir]
  (format "%s%scamelot%s%s" dir SystemUtils/FILE_SEPARATOR
          SystemUtils/FILE_SEPARATOR db-name))

(defn get-std-db-path
  "Return the OS-specific path to the database directory."
  []
  (cond
      SystemUtils/IS_OS_WINDOWS (db-path (env :localappdata))
      SystemUtils/IS_OS_LINUX (db-path (str (env :home) "/.local/share"))
      SystemUtils/IS_OS_MAC_OSX (db-path (str (env :home) "/Library/Application Support"))
      :else (db-path ".")))

(def ^:dynamic *db-override* nil)

(defn datadir-path
  []
  (or *db-override* (env :camelot-datadir)))

(defn get-db-path
  "Return the path to the database directory."
  []
  (if (datadir-path)
    (str (checked-datadir (datadir-path)) SystemUtils/FILE_SEPARATOR db-name)
    (get-std-db-path)))

(defn- media-path
  "Return the full path where imported media is stored."
  [dir]
  (format "%s%scamelot%s%s" dir SystemUtils/FILE_SEPARATOR
          SystemUtils/FILE_SEPARATOR media-directory-name))

(defn get-std-media-path
  "Return the OS specific path to the media directory."
  []
  (cond
      SystemUtils/IS_OS_WINDOWS (media-path (env :localappdata))
      SystemUtils/IS_OS_LINUX (media-path (str (env :home) "/.local/share"))
      SystemUtils/IS_OS_MAC_OSX (media-path (str (env :home) "/Library/Application Support"))
      :else (media-path ".")))

(defn get-media-path
  "Return the path to the media directory."
  []
  (if (datadir-path)
    (str (checked-datadir (datadir-path)) SystemUtils/FILE_SEPARATOR media-directory-name)
    (get-std-media-path)))

(defn- filestore-path
  "Return the full path where imported filestore is stored."
  [dir]
  (format "%s%scamelot%s%s" dir SystemUtils/FILE_SEPARATOR
          SystemUtils/FILE_SEPARATOR filestore-directory-name))

(defn get-std-filestore-path
  "Return the OS specific path to the filestore directory."
  []
  (cond
      SystemUtils/IS_OS_WINDOWS (filestore-path (env :localappdata))
      SystemUtils/IS_OS_LINUX (filestore-path (str (env :home) "/.local/share"))
      SystemUtils/IS_OS_MAC_OSX (filestore-path (str (env :home) "/Library/Application Support"))
      :else (filestore-path ".")))

(defn filestore-base-path
  "Return the base path to the filestore directory."
  []
  (if (datadir-path)
    (str (checked-datadir (datadir-path)) SystemUtils/FILE_SEPARATOR filestore-directory-name)
    (get-std-filestore-path)))

(defn- replace-unsafe-chars
  [filename]
  (str/replace filename #"(?i)[^a-z0-9 .-_]+" "-"))

(defn get-filestore-file-path
  "Return the path to the survey's filestore directory."
  [survey-id filename]
  (let [fs (str (filestore-base-path)
                SystemUtils/FILE_SEPARATOR survey-id
                SystemUtils/FILE_SEPARATOR (replace-unsafe-chars filename))
        parent (jf/get-parent-file (io/file fs))]
    (when (not (jf/exists? parent))
      (jf/mkdirs parent))
    (checked-datadir parent)
    fs))

(defn- serialise-dates
  "Convert configuration dates to long (e.g., for serialisation)."
  [config]
  (assoc config
         :project-start (tc/to-long (:project-start config))
         :project-end (tc/to-long (:project-end config))))

(defn- parse-dates
  "Convert configuration dates to DateTime objects."
  [config]
  (assoc config
         :project-start (tc/from-long (:project-start config))
         :project-end (tc/from-long (:project-end config))))

(defn- save-config-helper
  "Save the configuration data.  Overwrites the configuration file if the
  `overwrite?' flag is set."
  [config overwrite?]
  (let [conf (get-config-file)
        confdir (jf/get-parent-file (io/file conf))]
    (when-not (jf/exists? confdir)
      (jf/mkdirs confdir))
    (if (and (not overwrite?) (jf/exists? (io/file conf)))
      (throw (RuntimeException. (tr/translate config :problems/default-config-exists conf)))
      (with-open [w (io/writer conf)]
        (pp/write config :stream w)))
    config))

(defn- create-default-config
  "Save the default configuration to file.  Does not overwrite existing."
  []
  (save-config-helper default-config nil))

(defn- config-file-reader
  "Read the configuration file from storage,
Throws an IOException if the file cannot be read."
  [path]
  (if (jf/exists? (io/file path))
    (if (jf/readable? (io/file path))
      (io/reader path)
      (throw (IOException. (tr/translate default-config :problems/read-permission-denied path))))
    (do
      (create-default-config)
      (io/reader path))))

(defn- read-config
  "Application configuration"
  []
  (->> (get-config-file)
       (config-file-reader)
       (jf/pushback-reader)
       (edn/read)
       (parse-dates)))

(defn save-config
  "Save the configuration file.  Overwrites existing."
  [config]
  (save-config-helper (serialise-dates config) true))

(defn config
  "Return the configuration."
  ([]
   (merge (parse-dates default-config)
          (read-config)))
  ([session]
   (merge (parse-dates default-config)
          (read-config)
          session)))
