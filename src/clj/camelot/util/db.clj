(ns camelot.util.db
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as str]
   [clj-time.coerce :as tc]))

(defmacro with-transaction
  "Run `body' with a new transaction added to the binding for state."
  [[bind state] & body]
  `(jdbc/with-db-transaction [tx# (get-in ~state [:database :connection])]
     (let [~bind (assoc-in (assoc-in ~state [:database :default-connection]
                                     (get-in ~state [:database :connection]))
                           [:database :connection] tx#)]
       ~@body)))

(defmacro async-with-transaction
  "Run `body' with a new transaction, created from the default connection.

Intended for async operations which are already running within a transaction."
  [[bind state] & body]
  `(jdbc/with-db-transaction [tx# (get-in ~state [:database :connection])]
     (let [~bind (assoc-in ~state [:database :connection]
                         (get-in ~state [:database :default-connection]))]
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
  ([ks state q]
   (if-let [conn (get-in state [:database :connection])]
     (q ks {:connection conn})))
  ([state q]
   (if-let [conn (get-in state [:database :connection])]
     (q {} {:connection conn}))))

(defn with-db-keys
  "Run a function, translating the parameters and results as needed."
  [state f data]
  (-> data
      (db-keys)
      (with-connection state f)
      (clj-keys)))
