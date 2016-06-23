(ns camelot.db
  (:require [camelot.util.config :as settings]
            [clj-time.coerce :as tc]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [camelot.model.state :refer [State]]
            [schema.core :as s]
            [camelot.util.java-file :as f]
            [clojure.java.io :as io])
  (:import [java.io IOException]))

(defn db-path
  []
  (let [path (settings/get-db-path)
        fpath (io/file path)]
    (if (f/exists? fpath)
      (if (and (f/readable? fpath) (f/writable? fpath))
        fpath
        (throw (IOException. (str path ": Permission denied"))))
      (do
        (f/mkdirs fpath)
        path))))

(def spec
  "JDBC spec for the primary database."
  {:classname "org.apache.derby.jdbc.EmbeddedDriver",
   :subprotocol "derby",
   :subname (db-path),
   :create true})

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

(s/defn with-db-keys
  "Run a function, translating the parameters and results as needed."
  [state :- State
   f
   data]
  (-> data
      (db-keys)
      (with-connection (:connection state) f)
      (clj-keys)))
