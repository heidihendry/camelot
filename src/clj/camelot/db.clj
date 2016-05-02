(ns camelot.db
  (:require [ragtime.core :as rtc]
            [ragtime.jdbc :as jdbc]
            [clj-time.coerce :as tc]
            [clojure.string :as str]
            [camelot.processing.settings :as settings]))

(def spec {:classname "org.apache.derby.jdbc.EmbeddedDriver",
           :subprotocol "derby",
           :subname (settings/get-db-path),
           :create true})

(def config
  {:datastore (jdbc/sql-database spec)
   :migrations (jdbc/load-resources "migrations")})

(defn- clj-key
  [acc k v]
  (assoc acc (keyword (str/replace (name k) #"_" "-"))
         (if (instance? java.sql.Timestamp v)
           (tc/from-sql-time v)
           v)))

(defn clj-keys
  [data]
  (when-not (nil? data)
    (if (coll? data)
      (if (seq? data)
        (map #(into {} (reduce-kv clj-key {} %)) data)
        (into {} (reduce-kv clj-key {} data)))
      data)))

(defn- db-key
  [acc k v]
  (assoc acc (keyword (str/replace (name k) #"-" "_"))
         (if (instance? org.joda.time.DateTime v)
           (tc/to-sql-time v)
           v)))

(defn with-db-keys
  [f data]
  (clj-keys (f (reduce-kv db-key {} data))))

(defn migrate
  []
  (rtc/migrate-all (:datastore config)
                   (rtc/into-index (:migrations config))
                   (:migrations config)))

(defn rollback
  []
  (rtc/rollback-last (:datastore config)
                     (rtc/into-index (:migrations config))))
