(ns camelot.handler.cameras
  (:require [camelot.util.java-file :as jf]
            [camelot.db :as db]
            [yesql.core :as sql]
            [clojure.string :as s]
            [clojure.java.io :as f]))

(sql/defqueries "sql/cameras.sql" {:connection db/spec})

(defn db-key
  [acc k v]
  (assoc acc (keyword (s/replace (name k) #"-" "_")) v))

(defn with-db-keys
  [data f]
  (f (reduce-kv db-key {} data)))

(defn get-specific
  [state sid]
  (-get-specific {:camera-id sid}))

(defn get-specific-by-name
  [state sname]
  (with-db-keys {:camera-name sname} -get-specific-by-name))

(defn create!
  [state data]
  {:pre [(not (nil? (:camera-name data)))]}
  (if (not (empty? (get-specific-by-name state (:camera-name data))))
    ((:translate state) :camera/duplicate-name (:camera-name data))
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
