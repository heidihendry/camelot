(ns camelot.system.db.core
  (:require
   [camelot.util.state :as state]
   [camelot.system.protocols :as protocols]
   [clojure.tools.logging :as log]
   [yesql.core :as sql]
   [com.stuartsierra.component :as component]
   [clojure.java.jdbc :as jdbc]
   [schema.core :as sch])
  (:import
   (java.io IOException)))

(def ^:dynamic *migration-state* nil)

(def query-files
  ["bounding-box"
   "camera-status"
   "cameras"
   "deployments"
   "library"
   "media"
   "photos"
   "sighting-field"
   "sighting-field-value"
   "sightings"
   "sites"
   "species"
   "species-mass"
   "suggestion"
   "survey-file"
   "survey-sites"
   "survey-taxonomy"
   "surveys"
   "taxonomy"
   "trap-station-session-cameras"
   "trap-station-sessions"
   "trap-stations"])

(defn queries
  [name]
  (let [ns (create-ns (gensym "queryns-"))]
    (binding [*ns* ns]
      (sql/defqueries (str "sql/" name ".sql"))
      (->> (ns-publics ns)
           (map (fn [[k v]] (hash-map (keyword k) v)))
           (into {})))))

(defn build-queries
  []
  (->> query-files
       (map (fn [f] (hash-map (keyword f) (queries f))))
       (into {})))

(defn connect
  "Establish a connection to the database given a JDBC spec."
  [spec]
  (jdbc/get-connection spec)
  spec)

(defn close
  "Close a connection to the database given a JDBC spec."
  [spec]
  (try
    (let [spec (assoc (dissoc spec :create) :shutdown true)]
      (jdbc/get-connection spec)
      spec)
    (catch Exception e
      (log/info (.getMessage e)))))

(defrecord Database [config]
  protocols/Connectable
  (connect [this database]
    (let [spec (state/spec-for-database database)]
      (connect spec)))

  (disconnect [this database]
    (close (state/spec-for-database database)))

  component/Lifecycle
  (start [this]
    (assoc this
           :queries (build-queries)
           ;; TODO remove this
           :connections (map state/spec-for-dataset (vals (-> config :datasets)))))

  (stop [this]
    (assoc this :queries nil)))
