(ns camelot.system.db
  (:require
   [com.stuartsierra.component :as component]
   [camelot.system.db-migrate :as migrate]
   [clojure.java.jdbc :as jdbc]
   [schema.core :as s])
  (:import
   (java.io IOException)))

(defn connect
  "Establish a connection to the database given a JDBC spec."
  [spec]
  (jdbc/get-connection spec))

(defn close
  "Close a connection to the database given a JDBC spec."
  [spec]
  (try
    (jdbc/get-connection (assoc (dissoc spec :create) :shutdown true))
    (catch Exception e
      (println (.getMessage e)))))

(s/defrecord Database
    [connection :- clojure.lang.PersistentArrayMap]

  component/Lifecycle
  (start [this]
    (connect connection)
    (migrate/migrate connection)
    this)

  (stop [this]
    (close connection)
    (assoc this :connection nil)))
