(ns camelot.handler.sites
  (:require [camelot.util.java-file :as jf]
            [camelot.db :as db]
            [yesql.core :as sql]
            [clojure.string :as s]
            [clojure.java.io :as f]))

(sql/defqueries "sql/sites.sql" {:connection db/spec})

(defn clj-key
  [acc k v]
  (assoc acc (keyword (s/replace (name k) #"_" "-")) v))

(defn clj-keys
  [data]
  (if (nil? data)
    nil
    (into {} (reduce-kv clj-key {} data))))

(defn db-key
  [acc k v]
  (assoc acc (keyword (s/replace (name k) #"-" "_")) v))

(defn with-db-keys
  [data f]
  (f (reduce-kv db-key {} data)))

(defn clj-key
  [acc k v]
  (assoc acc (keyword (s/replace (name k) #"_" "-")) v))

(defn clj-keys
  [data]
  (reduce-kv clj-key {} data))

(defn get-specific
  [state sid]
  (clj-keys (first (-get-specific {:site_id sid}))))

(defn get-specific-by-name
  [state sname]
  (with-db-keys {:site-name sname} -get-specific-by-name))

(defn create!
  [state data]
  {:pre [(not (nil? (:site-name data)))]}
  (if (not (empty? (get-specific-by-name state (:site-name data))))
    ((:translate state) :site/duplicate-name (:site-name data))
    (with-db-keys data -create<!)))

(defn get-all
  [state]
  (-get-all))

(defn update!
  [state data]
  (-update! data))

(defn delete!
  [state id]
  (-delete! id))
