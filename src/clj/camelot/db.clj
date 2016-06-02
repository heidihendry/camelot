(ns camelot.db
  (:require [camelot.util.config :as settings]
            [clj-time.coerce :as tc]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [ragtime
             [core :as rtc]
             [jdbc :as ragjdbc]]))

(def spec
  "JDBC spec for the primary database."
  {:classname "org.apache.derby.jdbc.EmbeddedDriver",
   :subprotocol "derby",
   :subname (settings/get-db-path),
   :create true})

(def ragtime-config
  "Ragtime configuration"
  {:datastore (ragjdbc/sql-database spec)
   :migrations (ragjdbc/load-resources "migrations")})

(defmacro with-transaction
  "Run `body' with a new transaction added to the binding for state."
  [[bind state] & body]
  `(jdbc/with-db-transaction [tx# spec]
     (let [~bind (merge ~state {:connection tx#})]
       ~@body)))

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

(defn with-connection
  "Run a query with the given connection, if any."
  ([ks conn q]
   (if conn
     (q ks {:connection conn})
     (q ks)))
  ([conn q]
   (if conn
     (q {} {:connection conn})
     (q))))

(defn with-db-keys
  "Run a function, translating the parameters and results as needed."
  [state f data]
  (-> data
      (db-keys)
      (with-connection (:connection state) f)
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
                      (rtc/into-index (:migrations c))))
  ([c n]
   (rtc/rollback-last (:datastore c)
                      (rtc/into-index (:migrations c))
                      n)))
