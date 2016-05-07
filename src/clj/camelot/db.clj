(ns camelot.db
  (:require [ragtime.core :as rtc]
            [ragtime.jdbc :as jdbc]
            [clj-time.coerce :as tc]
            [clojure.string :as str]
            [camelot.processing.settings :as settings]))

(def spec
  "JDBC spec for the primary database."
  {:classname "org.apache.derby.jdbc.EmbeddedDriver",
   :subprotocol "derby",
   :subname (settings/get-db-path),
   :create true})

(def ragtime-config
  "Ragtime configuration"
  {:datastore (jdbc/sql-database spec)
   :migrations (jdbc/load-resources "migrations")})

(defn- clj-key
  "Reducer for translating from database types.
  Translates table column names and SQL data types."
  [acc k v]
  (assoc acc (keyword (str/replace (name k) #"_" "-"))
         (if (instance? java.sql.Timestamp v)
           (tc/from-sql-time v)
           v)))

(defn clj-keys
  "Translate from database types for any type of collection.
  Returns the given argument if it is not a collection."
  [data]
  (cond
      (seq? data) (map #(into {} (reduce-kv clj-key {} %)) data)
      (vector? data) (mapv #(into {} (reduce-kv clj-key {} %)) data)
      (map? data) (into {} (reduce-kv clj-key {} data))
      :else data))

(defn- db-key
  "Reducer for transforming data into database-suitable types."
  [acc k v]
  (assoc acc (keyword (str/replace (name k) #"-" "_"))
         (if (instance? org.joda.time.DateTime v)
           (tc/to-sql-time v)
           v)))

(defn db-keys
  "Translate data into database-suitable types."
  [data]
  (reduce-kv db-key {} data))

(defn with-db-keys
  "Run a function, translating the parameters and results as needed."
  [f data]
  (->> data
       (db-keys)
       (f)
       (clj-keys)))

(defn migrate
  "Apply the available database migrations."
  ([]
   (rtc/migrate-all (:datastore ragtime-config)
                    (rtc/into-index (:migrations ragtime-config))
                    (:migrations ragtime-config)))
  ([c]
   (rtc/migrate-all (:datastore c)
                    (rtc/into-index (:migrations c))
                    (:migrations c))))

(defn rollback
  "Rollback the last migration."
  ([]
   (rtc/rollback-last (:datastore ragtime-config)
                      (rtc/into-index (:migrations ragtime-config))))
  ([c]
   (rtc/rollback-last (:datastore c)
                      (rtc/into-index (:migrations c)))))
