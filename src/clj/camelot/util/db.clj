(ns camelot.util.db
  (:require
   [clojure.spec.alpha :as s]
   [camelot.state.datasets :as datasets]
   [camelot.util.state :as state]
   [camelot.spec.system :as sysspec]
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as str]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]))

(defmacro with-transaction
  "Run `body' with a new transaction added to the binding for state."
  [[bind state] & body]
  `(let [dataset-id# (state/get-dataset-id ~state)]
     (jdbc/with-db-transaction [tx# (datasets/lookup-connection (:datasets ~state))]
       (let [~bind (update-in ~state :datasets datasets/assoc-connection-context tx#)]
         ~@body))))

(defn- clj-key
  "Reducer for translating from database types.
  Translates table column names and SQL data types."
  [acc k v]
  (assoc acc (keyword (str/replace (name k) #"_" "-"))
         (if (and (re-find #"_(date|timestamp|created|updated)$" (name k))
                  v)
           (tc/from-long v)
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
           (tc/to-long v)
           v)))

(defn db-keys
  "Translate data into database-suitable types."
  [data]
  (assoc (reduce-kv db-key {} data)
         :current_timestamp (tc/to-long (t/now))))

(defn with-connection
  "Run a query with the given connection, if any."
  ([ks state q]
   (when-let [conn {:connection (datasets/lookup-connection (:datasets state))}]
     (q ks conn)))
  ([state q]
   (when-let [conn {:connection (datasets/lookup-connection (:datasets state))}]
     (q {} conn))))

(defn get-query
  [state scope qkey]
  (get-in state [:database :queries scope qkey]))

(s/fdef get-query
        :args (s/cat :state ::sysspec/state
                     :scope keyword?
                     :qkey keyword?)
        :ret fn?)

(defn with-db-keys
  "Run a function, translating the parameters and results as needed."
  [scope]
  (fn -with-db-keys
    ([state qkey data]
     (let [query (get-query state scope qkey)]
       (if (nil? query)
         (throw (IllegalArgumentException.
                 (format "Query '%s' not found in scope '%s'" qkey scope)))
         (-> data
             (db-keys)
             (with-connection state query)
             (clj-keys)))))
    ([state qkey]
     (-with-db-keys state qkey {}))))
