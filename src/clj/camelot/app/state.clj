(ns camelot.app.state
  "Application state."
  (:require
   [schema.core :as s]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [camelot.db.migrate :refer [migrate]]
   [camelot.db.core :as db]
   [camelot.util.file :as file]
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

(def config-store (atom {}))

(def default-config
  "Return the default configuration."
  {:erroneous-infrared-threshold 0.2
   :infrared-iso-value-threshold 999
   :language :en
   :send-usage-data false
   :root-path nil
   :night-end-hour 5
   :night-start-hour 21
   :project-start (tc/to-long (t/now))
   :project-end (tc/to-long (t/now))
   :sighting-independence-minutes-threshold 30
   :surveyed-species []
   :required-fields [[:headline] [:artist] [:phase] [:copyright]
                     [:location :gps-longitude] [:location :gps-latitude]
                     [:datetime] [:filename]]})

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

(defn get-os
  "Return a key representing the OS Camelot is running upon."
  []
  (cond
    SystemUtils/IS_OS_WINDOWS :windows
    SystemUtils/IS_OS_LINUX :linux
    SystemUtils/IS_OS_MAC_OSX :macosx
    :else :other))

(defn get-directory-separator
  "Return the path separator for the OS Camelot is running upon."
  []
  (if (= :windows (get-os))
    "\\"
    "/"))

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

(defn checked-datadir
  "Check the custom datadir, and return the canonicalised path.
  throws an IO Exception if unsuitable."
  [dir]
  (let [f (io/file dir)]
    (when (not (file/exists? f))
      (throw (IOException. (str "File does not exist: " (file/get-path f)))))
    (when (not (file/directory? f))
      (throw (IOException. (str "File is not a directory: " (file/get-path f)))))
    (when (or (not (file/readable? f)) (not (file/writable? f)))
      (throw (IOException. (str "Permission denied: " (file/get-path f)))))
    (file/get-path f)))

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
  (case (get-os)
    :windows (media-path (env :localappdata))
    :linux (media-path (str (env :home) "/.local/share"))
    :macosx (media-path (str (env :home) "/Library/Application Support"))
    :other (media-path ".")))

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
  (case (get-os)
    :windows (filestore-path (env :localappdata))
    :linux (filestore-path (str (env :home) "/.local/share"))
    :macosx (filestore-path (str (env :home) "/Library/Application Support"))
    :other (filestore-path ".")))

(defn filestore-base-path
  "Return the base path to the filestore directory."
  []
  (if (datadir-path)
    (str (checked-datadir (datadir-path)) SystemUtils/FILE_SEPARATOR filestore-directory-name)
    (get-std-filestore-path)))

(defn- replace-unsafe-chars
  [filename]
  (str/replace filename #"(?i)[^a-z0-9 .-_]+" "-"))

(defn get-filestore-survey-directory
  "Return the path to the survey's filestore directory."
  [survey-id]
  (io/file (filestore-base-path) (str survey-id)))

(defn get-filestore-file-path
  "Return the path to a file in the survey's filestore directory."
  [survey-id filename]
  (let [parent (get-filestore-survey-directory survey-id)
        fs (str (file/get-path parent) SystemUtils/FILE_SEPARATOR
                (replace-unsafe-chars filename))]
    (when (not (file/exists? parent))
      (file/mkdirs parent))
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
  "Save the configuration file.  Overwrites existing."
  [config]
  (let [sc (serialise-dates config)]
    (save-config-helper (assoc-root-dir sc) true)))

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

(defn gen-state*
  "Return the global application state, augmented with the default database
  connection."
  ([]
   {:config (config)
    :database {:connection spec}}))

(defn gen-state
  "Return the global application state.
Currently the only application state is the user's configuration."
  ([session]
   {:config (config session)})
  ([]
   {:config (config)}))

(s/defrecord Database
    [connection :- clojure.lang.PersistentArrayMap]

  component/Lifecycle
  (start [this]
    (db/connect connection)
    (migrate connection)
    this)

  (stop [this]
    (db/close connection)
    (assoc this :connection nil)))

(defn lookup [state k]
  (let [store (get-in state [:config :store])]
    (let [sv @store]
      (get (merge sv (or (:session state) {})) k))))

(defrecord Config [store config]
  component/Lifecycle
  (start [this]
    (reset! store config)
    this)

  (stop [this]
    (println "Stopping Config...")
    (reset! store {})
    (assoc this
           :store nil
           :config nil)))

(def State
  {(s/required-key :config) Config
   (s/required-key :database) s/Any
   (s/required-key :app) s/Any
   (s/optional-key :figwheel) s/Any
   (s/optional-key :session) s/Any
   (s/optional-key :jetty) s/Any
   (s/optional-key :camera-status-active-id) s/Int})
