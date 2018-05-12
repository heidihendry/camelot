(ns camelot.util.state
  "Application state."
  (:require
   [camelot.util.file :as file]
   [camelot.util.filesystem :as filesystem]
   [schema.core :as sch]
   [clj-time.core :as t]
   [clj-time.format :as tf]
   [clj-time.coerce :as tc]
   [com.stuartsierra.component :as component]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [environ.core :refer [env]])
  (:import
   (org.apache.commons.lang3 SystemUtils)
   (java.lang RuntimeException)
   (java.io IOException)))

(defonce config-store (atom {}))

(def default-config
  "Return the default configuration."
  {:media-importers 4
   :language :en
   :send-usage-data false
   :species-name-style :scientific
   :root-path nil
   :timezone nil
   :sighting-independence-minutes-threshold 30})

(def backup-timestamp-formatter (tf/formatter "YYYYMMddHHmmss"))
(def db-name "Database")
(def backup-dir-name "Backups")
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

(defn get-os
  "Return a key representing the OS Camelot is running upon."
  []
  (cond
    SystemUtils/IS_OS_WINDOWS :windows
    SystemUtils/IS_OS_LINUX :linux
    SystemUtils/IS_OS_MAC_OSX :macosx
    :else :other))

(defn get-config-location
  [loc-fn]
  (case (get-os)
    :windows (loc-fn (env :appdata))
    :linux (loc-fn (str (env :home) "/.config"))
    :macosx (loc-fn (str (env :home) "/Library/Preferences"))
    :other (loc-fn (str (env :pwd) ".camelot"))))

(defn get-config-file
  "Return the OS-specific path to the configuration file."
  []
  (get-config-location config-path))

(defn get-config-dir
  "Return the OS-specific path to the configuration directory."
  []
  (get-config-location config-dir))

(defn- db-path
  "Return the full path where the database is stored."
  [dir]
  (format "%s%scamelot%s%s" dir SystemUtils/FILE_SEPARATOR
          SystemUtils/FILE_SEPARATOR db-name))

(defn get-std-db-path
  "Return the OS-specific path to the database directory."
  []
  (case (get-os)
    :windows (db-path (env :localappdata))
    :linux (db-path (str (env :home) "/.local/share"))
    :macosx (db-path (str (env :home) "/Library/Application Support"))
    :other (db-path ".")))

(def ^:dynamic *db-override* "/home/chris/test2")

(defn datadir-path
  []
  (or *db-override* (env :camelot-datadir)))

(defn get-db-path
  "Return the path to the database directory."
  []
  (if (datadir-path)
    (str (filesystem/checked-dir (datadir-path)) SystemUtils/FILE_SEPARATOR db-name)
    (get-std-db-path)))

(defn- build-backup-dir-path
  "Return the full path where the backups is stored."
  [dir]
  (format "%s%scamelot%s%s" dir SystemUtils/FILE_SEPARATOR
          SystemUtils/FILE_SEPARATOR backup-dir-name))

(defn get-std-backup-dir-path
  "Return the OS-specific path to the database directory."
  []
  (case (get-os)
    :windows (build-backup-dir-path (env :localappdata))
    :linux (build-backup-dir-path (str (env :home) "/.local/share"))
    :macosx (build-backup-dir-path (str (env :home) "/Library/Application Support"))
    :other (build-backup-dir-path ".")))

(def ^:dynamic *backup-dir-override* nil)

(defn backup-dir-path
  []
  (or *backup-dir-override* (env :camelot-backup-dir)))

(defn get-backup-dir-path
  "Return the path to the Backups directory."
  []
  (if (backup-dir-path)
    (str (filesystem/checked-dir (backup-dir-path)) SystemUtils/FILE_SEPARATOR
         backup-dir-name)
    (get-std-backup-dir-path)))

(defn generate-backup-dirname
  []
  (str (get-backup-dir-path) SystemUtils/FILE_SEPARATOR
       (tf/unparse backup-timestamp-formatter (t/now))))

(defn- media-path
  "Return the full path where imported media is stored."
  [dir]
  (format "%s%scamelot%s%s" dir SystemUtils/FILE_SEPARATOR
          SystemUtils/FILE_SEPARATOR media-directory-name))

(defn get-std-media-path
  "Return the OS specific path to the media directory."
  []
  (case (get-os)
    :windows (media-path (env :localappdata))
    :linux (media-path (str (env :home) "/.local/share"))
    :macosx (media-path (str (env :home) "/Library/Application Support"))
    :other (media-path ".")))

(defn get-media-path
  "Return the path to the media directory."
  []
  (if (datadir-path)
    (str (filesystem/checked-dir (datadir-path))
         SystemUtils/FILE_SEPARATOR media-directory-name)
    (get-std-media-path)))

(defn- filestore-path
  "Return the full path where imported filestore is stored."
  [dir]
  (format "%s%scamelot%s%s" dir SystemUtils/FILE_SEPARATOR
          SystemUtils/FILE_SEPARATOR filestore-directory-name))

(defn get-std-filestore-path
  "Return the OS specific path to the filestore directory."
  []
  (case (get-os)
    :windows (filestore-path (env :localappdata))
    :linux (filestore-path (str (env :home) "/.local/share"))
    :macosx (filestore-path (str (env :home) "/Library/Application Support"))
    :other (filestore-path ".")))

(defn filestore-base-path
  "Return the base path to the filestore directory."
  []
  (if (datadir-path)
    (str (filesystem/checked-dir (datadir-path))
         SystemUtils/FILE_SEPARATOR filestore-directory-name)
    (get-std-filestore-path)))

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
        conftmp (str conf ".tmp")
        confdir (file/get-parent-file (io/file conf))]
    (when-not (file/exists? confdir)
      (file/mkdirs confdir))
    (if (and (not overwrite?) (file/exists? (io/file conf)))
      (throw (RuntimeException. "A default configuration file already exists."))
      (do
        (with-open [w (io/writer conftmp)]
          (pp/write config :stream w))
        (reset! config-store config)
        (file/rename (io/file conftmp) (io/file conf))))
    config))

(defn- create-default-config
  "Save the default configuration to file.  Does not overwrite existing."
  []
  (save-config-helper default-config nil))

(defn- config-file-reader
  "Read the configuration file from storage,
Throws an IOException if the file cannot be read."
  [path]
  (if (file/exists? (io/file path))
    (if (file/readable? (io/file path))
      (io/reader path)
      (throw (IOException. (str path ": Could not be read: permission denied"))))
    (do
      (create-default-config)
      (io/reader path))))

(defn- read-config
  "Application configuration"
  []
  (->> (get-config-file)
       (config-file-reader)
       (file/pushback-reader)
       (edn/read)
       (parse-dates)))

(defn config*
  "Return the configuration, though does not add a default root path."
  ([]
   (merge (parse-dates default-config)
          (read-config)))
  ([session]
   (merge (parse-dates default-config)
          (read-config)
          session)))

(defn config
  "Return the configuration."
  ([]
   (merge {:root-path (System/getProperty "user.dir")}
          (config*)))
  ([session]
   (merge {:root-path (System/getProperty "user.dir")}
          (config* session))))

(defn- assoc-root-dir
  [config]
  (let [rd (:root-dir (config*))]
    (if rd
      (assoc config :root-dir rd)
      config)))

(defn save-config
  "Save the configuration file. Overwrites existing."
  [config]
  (-> config
      serialise-dates
      assoc-root-dir
      (save-config-helper true)))

(defn final-db-path
  []
  (let [path (get-db-path)
        fpath (file/get-parent-file (io/file path))]
    (if (file/exists? fpath)
      (if (and (file/readable? fpath) (file/writable? fpath))
        path
        (throw (IOException. (str path ": Permission denied"))))
      (do
        (file/mkdirs fpath)
        path))))

(def spec
  "JDBC spec for the primary database."
  {:classname "org.apache.derby.jdbc.EmbeddedDriver",
   :subprotocol "derby",
   :subname (final-db-path),
   :create true})

(defn- create-missing
  "Create the directory for `path` should it not already exist."
  [path]
  (let [file (io/file path)]
    (or (file/exists? file) (file/mkdirs file))))

(defn path-map
  []
  (let [media-path (get-media-path)
        filestore-path (filestore-base-path)]
    (create-missing media-path)
    (create-missing filestore-path)
    {:filestore-base filestore-path
     :media media-path
     :database (get-db-path)
     :config (get-config-dir)}))

(defn lookup [state k]
  (let [store (get-in state [:config :store])]
    (let [sv @store]
      (get (merge sv (or (:session state) {})) k))))
