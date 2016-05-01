(ns camelot.processing.settings
  (:require [camelot.translation.core :refer :all]
            [camelot.util.java-file :as f]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [taoensso.tower :as tower]
            [environ.core :refer [env]])
  (:import [org.apache.commons.lang3 SystemUtils]
           [java.util Properties]))

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
                     [:datetime] [:filename]]
   :rename {:format "%s-%s"
            :fields [[:datetime] [:camera :model]]
            :date-format "YYYY-MM-dd HH.mm.ss"}})

(def os (System/getProperty "os.name"))
(def db-name "Database")

(defn db-path
  "Return the full path where the database is stored."
  [dir]
  (format "%s%scamelot%s%s" dir SystemUtils/FILE_SEPARATOR
          SystemUtils/FILE_SEPARATOR db-name))

(defn config-path
  "Return the full path where the configuration file is stored."
  [dir]
  (format "%s%scamelot%sconfig.clj" dir SystemUtils/FILE_SEPARATOR
          SystemUtils/FILE_SEPARATOR))

(defn get-db-path
  "Return the OS-specific path to the configuration file."
  []
  (cond
    SystemUtils/IS_OS_WINDOWS (db-path (env :localappdata))
    SystemUtils/IS_OS_LINUX (db-path (str (env :home) "/.local/share"))
    SystemUtils/IS_OS_MAC_OSX (db-path (str (env :home) "/Library/Application Support"))
    :else (db-path ".")))

(defn get-config-file
  "Return the OS-specific path to the configuration file."
  []
  (cond
    SystemUtils/IS_OS_WINDOWS (config-path (env :appdata))
    SystemUtils/IS_OS_LINUX (config-path (str (env :home) "/.config"))
    SystemUtils/IS_OS_MAC_OSX (config-path (str (env :home) "/Library/Preferences"))
    :else (config-path (str (env :pwd) ".camelot"))))

(def timestamp-formatter
  (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(defn normalise-dates
  "Convert configuration dates to long (e.g., for serialisation)."
  [config]
  (assoc config
         :project-start (tc/to-long (:project-start config))
         :project-end (tc/to-long (:project-end config))))

(defn parse-dates
  "Convert configuration dates to DateTime objects."
  [config]
  (assoc config
         :project-start (tc/from-long (:project-start config))
         :project-end (tc/from-long (:project-end config))))

(defn gen-translator
  "Create a translator for the user's preferred language."
  [config]
  (let [tlookup (partial (tower/make-t tconfig) (:language config))]
    (fn [t & vars]
      (apply format (tlookup t) vars))))

(defn- save-config-helper
  "Save the configuration data.  Overwrites the configuration file is the `overwrite?' flag is set."
  [config overwrite?]
  (let [translate (gen-translator config)
        conf (get-config-file)
        confdir (f/get-parent-file (io/file conf))]
    (when-not (f/exists? confdir)
      (f/mkdir confdir))
    (if (and (not overwrite?) (f/exists? (io/file conf)))
      (throw (RuntimeException. (translate :problems/default-config-exists conf)))
      (with-open [w (io/writer conf)]
        (pp/write config :stream w)))
    config))

(defn save-config
  "Save the configuration file.  Overwrites existing."
  [config]
  (save-config-helper (normalise-dates config) true))

(defn create-default-config
  "Save the default configuration file.  Does not overwrite existing."
  []
  (save-config-helper default-config nil))

(defn read-config-file
  "Read the configuration file from storage,
Throws a RuntimeException if the file cannot be read."
  [path]
  (if (and (f/exists? (io/file path)) (f/readable? (io/file path)))
    (io/reader path)
    (do
      (create-default-config)
      (io/reader path))))

(defn decursorise
  "Remove :value keys used for Om cursors to leaves from the configuration data."
  [conf]
  (into {} (map (fn [[k v]] {k (:value v)}) conf)))

(defn cursorise
  "Add :value keys used for Om cursors to leaves from the configuration data."
  [conf]
  (into {} (map (fn [[k v]] {k {:value v}}) conf)))

(defn config-internal
  "Application configuration"
  []
  (->> (get-config-file)
       (read-config-file)
       (f/pushback-reader)
       (edn/read)
       (parse-dates)))

(defn config
  "Return the configuration."
  []
  (merge (parse-dates default-config)
         (config-internal)))

(defn gen-state
  "Return the global application state."
  [conf]
  {:config conf
   :translate (gen-translator conf)})

(defn version-property-from-pom
  "Return a version string from the Jar metadata."
  [dep]
  (let [path (str "META-INF/maven/" (or (namespace dep) (name dep))
                  "/" (name dep) "/pom.properties")
        props (io/resource path)]
    (when props
      (with-open [stream (io/input-stream props)]
        (let [props (doto (Properties.) (.load stream))]
          (.getProperty props "version"))))))
