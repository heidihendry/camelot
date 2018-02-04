(ns camelot.util.db-migrate
  (:require
   [clojure.string :as str]
   [clojure.java.jdbc :as jdbc]
   [resauce.core :as resauce]
   [ragtime.core :as rtc]
   [ragtime.protocols :as rtp])
  (:use
   [ragtime.jdbc :as ragjdbc]))

(defn- clj-migration-file-parts [file]
  (rest (re-matches #"(.*?)\.(up|down)(?:\.(\d+))?\.clj" (str file))))

(defrecord CamelotMigration [id up down]
  rtp/Migration
  (id [_] id)
  (run-up!   [_ db] (doseq [c up] (load-string c)))
  (run-down! [_ db] (doseq [c down] (load-string c))))

;; Work around for https://github.com/weavejester/ragtime/issues/103
(let [pattern (re-pattern (str "([^\\/]*)\\/?$"))]
  (defn- -resource-basename
    [file]
    (second (re-find pattern (str file)))))
(intern 'ragtime.jdbc 'basename #'-resource-basename)

(defn camelot-migration
  [migration-map]
  (map->CamelotMigration migration-map))

(defmethod load-files ".clj" [files]
  (for [[id files] (->> files
                        (group-by (comp first clj-migration-file-parts))
                        (sort-by key))]
    (let [{:strs [up down]} (group-by (comp second clj-migration-file-parts) files)]
      (camelot-migration
       {:id   (-resource-basename id)
        :up   (mapv slurp up)
        :down (mapv slurp down)}))))

(defn- tx-execute-sql! [db-spec statements]
  (jdbc/with-db-transaction [tx db-spec]
    (doseq [s statements]
      (jdbc/execute! tx [s]))))

(defrecord TxSqlMigration [id up down]
  rtp/Migration
  (id [_] id)
  (run-up!   [_ db] (tx-execute-sql! (:db-spec db) up))
  (run-down! [_ db] (tx-execute-sql! (:db-spec db) down)))

(alter-meta! #'->TxSqlMigration assoc :no-doc true)
(alter-meta! #'map->TxSqlMigration assoc :no-doc true)

(defn tx-sql-migration
  "Create a Ragtime migration from a map with a unique :id, and :up and :down
  keys that map to ordered collection of SQL strings."
  [migration-map]
  (map->TxSqlMigration migration-map))

(defn- sql-file-parts [file]
  (rest (re-matches #"(.*?)\.(up|down)(?:\.(\d+))?\.sql" (str file))))

(defn- read-sql [file]
  (str/split (slurp file) #"(?m)\n\s*--;;\s*\n"))

(defmethod load-files ".sql" [files]
  (for [[id files] (->> files
                        (group-by (comp first sql-file-parts))
                        (sort-by key))]
    (let [{:strs [up down]} (group-by (comp second sql-file-parts) files)]
      (tx-sql-migration
       {:id   (-resource-basename id)
        :up   (vec (mapcat read-sql (sort-by str up)))
        :down (vec (mapcat read-sql (sort-by str down)))}))))

(defn ragtime-config
  "Ragtime configuration"
  [connection]
  {:datastore (ragjdbc/sql-database connection)
   :migrations (sort-by :id (ragjdbc/load-resources "migrations"))})

(defn migrate
  "Apply the available database migrations."
  ([connection]
   (let [conf (ragtime-config connection)]
     (rtc/migrate-all (:datastore conf)
                      (rtc/into-index (:migrations conf))
                      (:migrations conf)))))

(defn latest-available-version
  [conn]
  (some->> conn
           ragtime-config
           :migrations
           (map :id)
           last
           (take-while #(not= % \-))
           (str/join "")))

(defn version
  "Return the prefix of the latest migration name as a string, e.g., '019'.
This will return nil if a migration has never been applied."
  [connection]
  (let [conf (ragtime-config connection)]
    (some->> conf
             :migrations
             rtc/into-index
             (ragtime.core/applied-migrations (:datastore conf))
             (map :id)
             last
             (take-while #(not= % \-))
             (str/join ""))))

(defn migrations-available?
  "Returns `true` if a migration with a newer version than currently applied
  to the database is available. `false` otherwise."
  [connection]
  (not= (version connection)
        (latest-available-version connection)))

(defn rollback
  "Rollback the last migration."
  ([connection]
   (let [conf (ragtime-config connection)]
     (rtc/rollback-last (:datastore conf)
                        (rtc/into-index (:migrations conf)))))
  ([connection n]
   (let [conf (ragtime-config connection)]
     (rtc/rollback-last (:datastore conf)
                        (rtc/into-index (:migrations conf))
                        n))))
