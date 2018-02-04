(ns camelot.system.db
  (:require
   [yesql.core :as sql]
   [com.stuartsierra.component :as component]
   [clojure.java.jdbc :as jdbc]
   [schema.core :as s])
  (:import
   (java.io IOException)))

(def query-files
  ["maintenance"
   "cameras"
   "camera-status"
   "deployments"
   "library"
   "media"
   "photos"
   "sighting-field"
   "sighting-field-value"
   "sightings"
   "sites"
   "species-mass"
   "species"
   "survey-file"
   "survey-sites"
   "surveys"
   "survey-taxonomy"
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
  (jdbc/get-connection spec))

(defn close
  "Close a connection to the database given a JDBC spec."
  [spec]
  (try
    (jdbc/get-connection (assoc (dissoc spec :create) :shutdown true))
    (catch Exception e
      (println (.getMessage e)))))

(s/defrecord Database
    [connection :- clojure.lang.PersistentArrayMap]

  component/Lifecycle
  (start [this]
    (connect connection)
    (assoc this :queries (build-queries)))

  (stop [this]
    (when connection
      (close connection))
    (-> this
        (assoc :connection nil)
        (assoc :queries nil))))
