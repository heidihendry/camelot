(ns camelot.handler.surveys
  (:require [camelot.util.java-file :as jf]
            [camelot.db :as db]
            [clojure.string :as s]
            [yesql.core :as sql]
            [clojure.java.io :as f]))

(sql/defqueries "sql/surveys.sql" {:connection db/spec})

(defn clj-key
  [acc k v]
  (assoc acc (keyword (s/replace (name k) #"_" "-")) v))

(defn clj-keys
  [data]
  (if (nil? data)
    nil
    (into {} (reduce-kv clj-key {} data))))

(defn- check-directory
  [state sdir]
  (let [dh (f/file sdir)]
    (cond
      (not (jf/exists? dh)) ((:translate state) :problems/path-not-found sdir)
      (not (jf/directory? dh)) ((:translate state) :problems/not-directory sdir)
      (not (jf/readable? dh)) ((:translate state) :problems/read-permission-denied sdir)
      :else nil)))

(defn get-specific
  [state sid]
  (clj-keys (first (-get-specific {:id sid}))))

(defn get-specific-by-name
  [state sname]
  (-get-specific-by-name {:name sname}))

(defn create!
  [state sname sdir]
  {:pre [(not (nil? sname))]}
  (if (not (empty? (get-specific-by-name state sname)))
    ((:translate state) :survey/duplicate-name sname)
    (let [err (check-directory state sdir)]
      (if err
        err
        (-create<! {:name sname :directory sdir})))))

(defn get-all
  [state]
  (-get-all))

(defn update!
  [state sid sname sdir]
  (let [err (check-directory state sdir)]
    (if err
      err
      (-update! {:id sid :name sname :directory sdir}))))

(defn delete!
  [state id]
  (-delete! id))
