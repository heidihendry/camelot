(ns camelot.system.migrater
  (:require
   [clojure.java.io :as io]
   [camelot.util.file :as file]
   [camelot.system.protocols :as protocols]
   [camelot.state.datasets :as datasets]
   [camelot.state.database :as database]
   [camelot.util.db-migrate :as db-migrate]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component])
  (:import
   (java.io IOException)))

(do
  (create-ns 'camelot.migration)
  (.setDynamic (intern 'camelot.migration '*dataset*))
  (.setDynamic (intern 'camelot.migration '*connection*)))

(def ^:private derby-container-dir "seg0")

(defn upgrade-plan
  "Return a map describing any upgrade.
  Contains key `:from`, which is the version the database is currently at, and
  `:to`, which is the version the latest version the database can be upgraded
  to."
  [state]
  (let [conn (datasets/lookup-connection (:datasets state))]
    {:from (db-migrate/version conn)
     :to (db-migrate/latest-available-version conn)}))

(defn is-db-initialised?
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
  (if (is-db-initialised? spec)
    {:from (db-migrate/version spec)
     :to (db-migrate/latest-available-version spec)}
    {:to (db-migrate/latest-available-version spec)}))

(defn migrate
  "Upgrade the database."
  [dataset]
  (when-let [db-conn (database/spec-for-dataset dataset)]
    (binding [camelot.migration/*dataset* dataset
              camelot.migration/*connection* db-conn]
      (db-migrate/migrate db-conn))))

(defn rollback
  "Rollback the database."
  [dataset]
  (when-let [db-conn (database/spec-for-dataset dataset)]
    (binding [camelot.migration/*dataset* dataset
              camelot.migration/*connection* db-conn]
      (db-migrate/rollback db-conn))))

(defn- prepare-migration-plan
  [dataset]
  (let [plan (upgrade-plan-for-spec (database/spec-for-dataset dataset))]
    (assoc plan :backup (not= (:from plan) (:to plan)))))

(defn- plan-backup-dataset!
  [plan backup]
  (when (:backup plan)
    (backup)))

(defn- plan-migrate-dataset!
  [plan dataset]
  (when (not= (:from plan) (:to plan))
    (log/info (format "Upgrading dataset %s..." (:name dataset)))
    (migrate dataset)
    (log/info (format "Dataset %s up-to-date (%s)."
                      (:name dataset)
                      (let [spec (database/spec-for-dataset dataset)]
                        (:to (upgrade-plan-for-spec spec)))))))

(defrecord Migrater [backup-manager]
  protocols/Migratable
  (migrate [this dataset]
    (-> (prepare-migration-plan dataset)
        (plan-backup-dataset! #(.backup backup-manager dataset))
        (plan-migrate-dataset! dataset)))

  (rollback [this dataset]
    (rollback dataset))

  component/Lifecycle
  (start [this]
    this)

  (stop [this]
    this))
