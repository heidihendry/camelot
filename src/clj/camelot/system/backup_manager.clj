(ns camelot.system.backup-manager
  (:require
   [camelot.system.protocols :as protocols]
   [com.stuartsierra.component :as component]
   [camelot.util.file :as file]
   [camelot.state.database :as database]
   [clj-time.core :as t]
   [clj-time.format :as tf]
   [clojure.java.io :as io])
  (:import
   (java.util.zip ZipEntry ZipOutputStream)))

(def ^:private backup-timestamp-formatter
  (tf/formatter "YYYYMMddHHmmss"))

(defn- compress-dir
  [dir]
  (let [zip-path (str dir ".zip")]
    (with-open [zip (ZipOutputStream. (io/output-stream (str dir ".zip")))]
      (doseq [f (file-seq (io/file dir)) :when (file/file? f)]
        (.putNextEntry zip (ZipEntry. ^String (file/get-path f)))
        (io/copy f zip)
        (.closeEntry zip)))
    zip-path))

(defn- generate-backup-dirname
  [dataset]
  (file/mkdirs (-> dataset :paths :backup))
  (io/file (-> dataset :paths :backup)
           (tf/unparse backup-timestamp-formatter (t/now))))

(defrecord BackupManager [database]
  protocols/BackupManager
  (backup [this dataset]
    (let [backup! (-> database :queries :maintenance)
          backup-dir (generate-backup-dirname dataset)
          spec (database/spec-for-dataset dataset)]
      (backup! {:path (.getPath backup-dir)} {:connection spec})
      (let [zip (compress-dir backup-dir)]
        (file/delete-recursive (io/file backup-dir))
        zip)))

  component/Lifecycle
  (start [this]
    this)

  (stop [this]
    this))
