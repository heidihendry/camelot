(ns camelot.util.state
  "Application state."
  (:require
   [camelot.market.config :as market-config]
   [clj-time.core :as t]
   [clj-time.format :as tf]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn- deep-merge
  "Merge maps in `ms` recursively"
  [& ms]
  (apply merge-with (fn [x y]
                      (if (map? y)
                        (deep-merge x y)
                        y))
         ms))

(def config-cache (atom nil))

(defn paths-to-file-objects
  [m]
  (letfn [(to-file-objects [p] (into {} (map (fn [[k v]] [k (io/file v)]) p)))]
    (update m :paths to-file-objects)))

(defn config-file-objects
  [config]
  (letfn [(dataset-paths-to-file-objects [ds]
            (reduce-kv (fn [acc k v] (assoc acc k (paths-to-file-objects v)))
                       {} ds))]
    (-> config
        paths-to-file-objects
        (update :datasets dataset-paths-to-file-objects))))

(defn- select-config
  [config dataset]
  (-> config
      (dissoc :datasets)
      (merge (get-in config [:datasets dataset]))))

(defn- read-config
  []
  (when (nil? @config-cache)
    (reset! config-cache
            (config-file-objects (market-config/read-config))))
  @config-cache)

(defn get-config
  [dataset]
  (select-config (read-config) dataset))

(defn system-config
  []
  (let [config (read-config)]
    (-> config
        (assoc :dataset-ids (sort-by (comp str/lower-case name)
                                     (keys (:datasets config)))))))

(defn get-dataset-ids
  [state]
  (-> state :config :dataset-ids))

(def ^:private backup-timestamp-formatter
  (tf/formatter "YYYYMMddHHmmss"))

(defn get-dataset
  [state dataset-id]
  (if-let [dataset (get-in state [:config :datasets dataset-id])]
    dataset
    (throw (ex-info "Dataset not found" {:dataset-id dataset-id}))))

(defn get-dataset-id [state]
  (if-let [dataset-id (get-in state [:session :dataset-id])]
    dataset-id
    (throw (ex-info "Dataset ID not set" {}))))

(defn lookup [state k]
  (let [dataset-id (get-dataset-id state)
        dataset (get-dataset state dataset-id)]
    (get (merge (:config state) dataset (or (:session state) {})) k)))

(defn lookup-path [state k]
  (get (lookup state :paths) k))

(defn lookup-connection [state]
  (let [dataset-id (get-dataset-id state)]
    (if-let [conn (get-in state [:database :connections dataset-id])]
      conn
      (throw (ex-info "Database connection not found" {:dataset-id dataset-id})))))

(defn generate-backup-dirname
  [state]
  (io/file (lookup-path state :backup)
           (tf/unparse backup-timestamp-formatter (t/now))))

(defn with-session
  "Return a copy of `state` with `session-data` added to the session."
  [state session-data]
  (update state :session #(merge % session-data)))

(defn with-dataset
  "Return a copy of `state` with the `dataset-id` set."
  [state dataset-id]
  (if-let [current-dataset-id (get-in state [:session :dataset-id])]
    (throw (ex-info "Dataset ID already set" {:current-dataset-id current-dataset-id
                                              :requested-dataset-id dataset-id}))
    (assoc-in state [:session :dataset-id] dataset-id)))

(defn map-datasets
  [f system-state]
  (map #(let [state (with-dataset system-state %)]
          (f state))
       (get-dataset-ids system-state)))

(defn dissoc-dataset
  [state]
  (update state :session dissoc :dataset-id))

(defn config->state
  "Lift config to a state-like map."
  [config]
  {:config config})

(defn spec
  "JDBC spec for the database."
  [state]
  {:classname "org.apache.derby.jdbc.EmbeddedDriver",
   :subprotocol "derby",
   :subname (.getPath (io/file (lookup-path state :database)
                               ;; TODO this should be in camelot market
                               "Database")),
   :create true})
