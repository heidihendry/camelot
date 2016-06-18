(ns camelot.migrate
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [resauce.core :as resauce]
            [camelot.db :as db]
            [ragtime
             [core :as rtc]
             [protocols :as rtp]])
  (:use [ragtime.jdbc :as ragjdbc]))

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

(def ragtime-config
  "Ragtime configuration"
  {:datastore (ragjdbc/sql-database db/spec)
   :migrations (sort-by :id (ragjdbc/load-resources "migrations"))})

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

(defn version
  "Return the prefix of the latest migration name as a string, e.g., '019'.
This will return nil if a migration has never been applied."
  []
  (some->> ragtime-config
           :migrations
           rtc/into-index
           (ragtime.core/applied-migrations (:datastore ragtime-config))
           (map :id)
           last
           (take-while #(not= % \-))
           (str/join "")))

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
