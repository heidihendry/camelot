(ns camelot.util.state
  "Application state."
  (:require
   [camelot.market.config :as market-config]
   [clj-time.core :as t]
   [clj-time.format :as tf]
   [clojure.java.io :as io]))

(def config-cache (atom nil))

(defn read-config
  []
  (when (nil? @config-cache)
    (reset! config-cache (market-config/read-config)))
  @config-cache)

(def ^:private backup-timestamp-formatter
  (tf/formatter "YYYYMMddHHmmss"))

(defn lookup [state k]
  (let [config (get state :config)]
    (get (merge config (or (:session state) {})) k)))

(defn lookup-path [state k]
  (get (lookup state :paths) k))

(defn generate-backup-dirname
  [state]
  (io/file (lookup-path state :backup)
           (tf/unparse backup-timestamp-formatter (t/now))))

(def spec
  "JDBC spec for the primary database."
  {:classname "org.apache.derby.jdbc.EmbeddedDriver",
   :subprotocol "derby",
   :subname (get-in (read-config) [:paths :database]),
   :create true})
