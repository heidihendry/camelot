(ns camelot.util.maintenance
  (:require
   [camelot.system.db.core :as sysdb]
   [camelot.util.file :as file]
   [clj-time.core :as t]
   [clj-time.format :as tf]
   [clojure.java.io :as io]
   [camelot.util.db :as dbutil]
   [camelot.util.state :as state]
   [camelot.util.db-migrate :as db-migrate]
   [clojure.tools.logging :as log]
   [yesql.core :as sql])
  (:import
   (java.io IOException)
   (java.util.zip ZipEntry ZipOutputStream)))

(sql/defqueries (str "sql/maintenance.sql"))

(def ^:private backup-timestamp-formatter
  (tf/formatter "YYYYMMddHHmmss"))

(def ^:private derby-container-dir "seg0")
(def ^:private query (dbutil/with-db-keys :maintenance))

;; TODO kill this
(defn migrations-available?
  "Returns `true` if a migration with a newer version than currently applied
  to the database is available. `false` otherwise."
  [state]
  (let [conn (state/lookup-connection state)]
    (db-migrate/migrations-available? conn)))

(defn migrations-available-for-spec?
  "Returns `true` if a migration with a newer version than currently applied
  to the given database are available. `false` otherwise."
  [spec]
  (db-migrate/migrations-available? spec))

(defn upgrade-plan
  "Return a map describing any upgrade.
  Contains key `:from`, which is the version the database is currently at, and
  `:to`, which is the version the latest version the database can be upgraded
  to."
  [state]
  (let [conn (state/lookup-connection state)]
    {:from (db-migrate/version conn)
     :to (db-migrate/latest-available-version conn)}))

;; TODO kill this
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

(defn is-db-initialised-for-spec?
  "Returns `true` if the database looks initialized. `false` otherwise.
  Based on a simple directory heuristic. Don't trust it with your life."
  [spec]
  (try
    (let [path (io/file (:subname spec))
          cdir (io/file path derby-container-dir)]
      (and (file/exists? cdir)
           (file/directory? cdir)))
    (catch IOException _
      false)))

(defn upgrade-plan-for-spec
  "Return a map describing any upgrade.
  Contains key `:from`, which is the version the database is currently at, and
  `:to`, which is the version the latest version the database can be upgraded
  to."
  [spec]
  (if (is-db-initialised-for-spec? spec)
    {:from (db-migrate/version spec)
     :to (db-migrate/latest-available-version spec)}
    {:to (db-migrate/latest-available-version spec)}))

(defn- compress-dir
  [dir]
  (let [zip-path (str dir ".zip")]
    (with-open [zip (ZipOutputStream. (io/output-stream (str dir ".zip")))]
      (doseq [f (file-seq (io/file dir)) :when (file/file? f)]
        (.putNextEntry zip (ZipEntry. ^String (file/get-path f)))
        (io/copy f zip)
        (.closeEntry zip)))
    zip-path))

(defn generate-backup-dirname
  [dataset]
  (io/file (-> dataset :paths :backup)
           (tf/unparse backup-timestamp-formatter (t/now))))

(defn backup
  "Back up the database."
  [dataset]
  (let [backup-dir (generate-backup-dirname dataset)
        spec (state/spec-for-dataset dataset)]
    (backup! {:path (.getPath backup-dir)} {:connection spec})
    (let [zip (compress-dir backup-dir)]
      (file/delete-recursive (io/file backup-dir))
      zip)))

(defn migrate
  "Upgrade the database."
  [state]
  ;; TODO kill migration state
  (binding [sysdb/*migration-state* state]
    (when-let [db-conn (state/lookup-connection state)]
      (db-migrate/migrate db-conn))))

(defn migrate-dataset
  "Upgrade the database."
  [dataset]
  ;; TODO kill migration state
  (binding [sysdb/*migration-state* {}]
    (when-let [db-conn (state/spec-for-dataset dataset)]
      (db-migrate/migrate db-conn))))

(defn rollback
  "Rollback the database."
  [state]
  ;; TODO kill migration state
  (binding [sysdb/*migration-state* state]
    (when-let [db-conn (state/lookup-connection state)]
      (db-migrate/rollback db-conn))))

(defn rollback-dataset
  "Rollback the database."
  [dataset]
  ;; TODO kill migration state
  (binding [sysdb/*migration-state* {}]
    (when-let [db-conn (state/spec-for-dataset dataset)]
      (db-migrate/rollback db-conn))))

(defn- prepare-migration-plan
  [dataset]
  (let [plan (upgrade-plan-for-spec (state/spec-for-dataset dataset))]
    (assoc plan :backup (not= (:from plan) (:to plan)))))

;; TODO flesh this out to actually backup the dataset
(defn- plan-backup-dataset!
  [plan dataset]
  (when (:backup plan)
    (backup dataset)))

(defn- plan-migrate-dataset!
  [plan dataset]
  (when (not= (:from plan) (:to plan))
    (log/info (format "Upgrading dataset %s..." (:name dataset)))
    (migrate dataset)
    (log/info (format "Dataset %s up-to-date (%s)."
                      (:name dataset)
                      (let [spec (state/spec-for-dataset dataset)]
                        (:to (upgrade-plan-for-spec spec)))))))

(defn safe-migrate!
  "Safely migrate spec, taking a backup if needed."
  [dataset]
  (-> (prepare-migration-plan dataset)
      (plan-backup-dataset! dataset)
      (plan-migrate-dataset! dataset)))
