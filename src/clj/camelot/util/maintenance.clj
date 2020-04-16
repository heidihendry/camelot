(ns camelot.util.maintenance
  (:require
   [camelot.system.db.core :as sysdb]
   [camelot.util.file :as file]
   [clojure.java.io :as io]
   [camelot.util.db :as dbutil]
   [camelot.util.state :as state]
   [camelot.util.db-migrate :as db-migrate])
  (:import
   (java.io IOException)
   (java.util.zip ZipEntry ZipOutputStream)))

(def ^:private derby-container-dir "seg0")
(def ^:private query (dbutil/with-db-keys :maintenance))

(defn migrations-available?
  "Returns `true` if a migration with a newer version than currently applied
  to the database is available. `false` otherwise."
  [state]
  (let [conn (state/lookup-connection state)]
    (db-migrate/migrations-available? conn)))

(defn upgrade-plan
  "Return a map describing any upgrade.
  Contains key `:from`, which is the version the database is currently at, and
  `:to`, which is the version the latest version the database can be upgraded
  to."
  [state]
  (let [conn (state/lookup-connection state)]
    {:from (db-migrate/version conn)
     :to (db-migrate/latest-available-version conn)}))

(defn is-db-initialised?
  "Returns `true` if the database looks initialized. `false` otherwise.
  Based on a simple directory heuristic. Don't trust it with your life."
  [state]
  (try
    (let [path (state/lookup-path state :database)
          cdir (io/file path derby-container-dir)]
      (and (file/exists? cdir)
           (file/directory? cdir)))
    (catch IOException _
      false)))

(defn- compress-dir
  [dir]
  (let [zip-path (str dir ".zip")]
    (with-open [zip (ZipOutputStream. (io/output-stream (str dir ".zip")))]
      (doseq [f (file-seq (io/file dir)) :when (file/file? f)]
        (.putNextEntry zip (ZipEntry. ^String (file/get-path f)))
        (io/copy f zip)
        (.closeEntry zip)))
    zip-path))

(defn backup
  "Back up the database."
  [state]
  (let [backup-dir (state/generate-backup-dirname state)]
    (query state :backup! {:path (.getPath backup-dir)})
    (let [zip (compress-dir backup-dir)]
      (file/delete-recursive (io/file backup-dir))
      zip)))

(defn migrate
  "Upgrade the database."
  [state]
  (binding [sysdb/*migration-state* state]
    (if-let [db-conn (state/lookup-connection state)]
      (db-migrate/migrate db-conn))))

(defn rollback
  "Rollback the database."
  [state]
  (binding [sysdb/*migration-state* state]
    (if-let [db-conn (state/lookup-connection state)]
      (db-migrate/rollback db-conn))))
