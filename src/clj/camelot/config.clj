(ns camelot.config
  (:require [camelot.translations.core :refer :all]
            [camelot.util.java-file :as f]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clojure.pprint :as pp]
            [clj-time.format :as tf]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [taoensso.tower :as tower]
            [environ.core :refer [env]])
  (:import [org.apache.commons.lang3 SystemUtils]))

(def default-config
  {:erroneous-infrared-threshold 0.2
   :infrared-iso-value-threshold 999
   :language :en
   :night-end-hour 5
   :night-start-hour 21
   :project-start "1970-01-01 00:00:00"
   :project-end "2069-12-31 23:59:59"
   :sighting-independence-minutes-threshold 20
   :surveyed-species []
   :required-fields [[:headline] [:artist] [:phase] [:copyright]
                     [:location :gps-longitude] [:location :gps-longitude-ref]
                     [:location :gps-latitude] [:location :gps-latitude-ref]
                     [:datetime] [:filename]]
   :rename {:format "%s-%s"
            :fields [[:datetime] [:camera :model]]
            :date-format "YYYY-MM-dd HH.mm.ss"}})

(def os (System/getProperty "os.name"))

(defn config-path
  [dir]
  (format "%s%scamelot%sconfig.clj" dir SystemUtils/FILE_SEPARATOR SystemUtils/FILE_SEPARATOR))

(defn get-config-file
  []
  (cond
    SystemUtils/IS_OS_WINDOWS (env :appdata)
    SystemUtils/IS_OS_LINUX (config-path (str (env :home) "/.config"))
    SystemUtils/IS_OS_MAC_OSX (config-path (str (env :home) "/Library/Preferences"))
    :else (config-path (str (env :pwd) ".camelot"))))

(def timestamp-formatter (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(defn parse-dates
  [config]
  (assoc config
         :project-start (tf/parse timestamp-formatter (:project-start config))
         :project-end (tf/parse timestamp-formatter (:project-end config))))

(def config-cache (atom nil))

(defn gen-translator
  "Create a translator for the user's preferred language."
  [config]
  (let [tlookup (partial (tower/make-t tconfig) (:language config))]
    (fn [t & vars]
      (apply format (tlookup t) vars))))

(defn read-config-file
  [path]
  (if (f/can-read? (io/file path))
    (io/reader path)
    (throw (RuntimeException. ((gen-translator default-config) :problems/config-not-found)))))

(defn config
  "Application configuration"
  []
  (->> (get-config-file)
       (read-config-file)
       (f/pushback-reader)
       (edn/read)
       (parse-dates)
       (reset! config-cache)))

(defn create-default-config
  []
  (let [translate (gen-translator default-config)
        conf (get-config-file)
        confdir (f/getParentFile (io/file conf))]
    (when (not (f/exists confdir))
      (f/mkdir confdir))
    (if (f/exists (io/file conf))
      (throw (RuntimeException. (translate :problems/default-config-exists conf)))
      (do
        (with-open [w (io/writer conf)]
          (pp/write default-config :stream w))
        (println (translate :default-config-created conf))))))

(defn gen-state
  "Return the global application state."
  [conf]
  {:config conf
   :translate (gen-translator conf)})
