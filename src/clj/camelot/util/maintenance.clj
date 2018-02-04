(ns camelot.util.maintenance
  (:require
   [yesql.core :as sql]
   [clojure.string :as cstr]
   [camelot.util.file :as file]
   [clojure.java.io :as io]
   [camelot.util.db :as dbutil]
   [camelot.util.state :as state]
   [camelot.util.db-migrate :as db-migrate]
   [com.stuartsierra.component :as component])
  (:import
   (java.io IOException)))

(def ^:private derby-container-dir "seg0")
(def ^:private query (dbutil/with-db-keys :maintenance))

(defn migrations-available?
  "Returns `true` if a migration with a newer version than currently applied
  to the database is available. `false` otherwise."
  [state]
  (let [conn (-> state :database :connection)]
    (db-migrate/migrations-available? conn)))

(defn upgrade-plan
  "Return a map describing any upgrade.
  Contains key `:from`, which is the version the database is currently at, and
  `:to`, which is the version the latest version the database can be upgraded
  to."
  [state]
  (let [conn (-> state :database :connection)]
    {:from (db-migrate/version conn)
     :to (db-migrate/latest-available-version conn)}))

(defn is-db-initialised?
  "Returns `true` if the database looks initialized. `false` otherwise.
  Based on a simple directory heuristic. Don't trust it with your life."
  []
  (try
    (let [path (state/get-db-path)
          cdir (io/file path derby-container-dir)]
      (and (file/exists? cdir)
           (file/directory? cdir)))
    (catch IOException e
      false)))

(defn backup
  "Back up the database."
  [state]
  (query state :backup! {:path (state/generate-backup-dirname)}))

(defn migrate
  "Upgrade the database."
  [state]
  (db-migrate/migrate (-> state :database :connection)))
