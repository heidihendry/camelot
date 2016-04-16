(ns camelot.db
  (:require [ragtime.jdbc :as rt]
            [ragtime.repl :as rtr]))

(def spec {:classname "org.apache.derby.jdbc.EmbeddedDriver",
           :subprotocol "derby",
           :subname "MyDB",
           :create true})

(defn connection-uri
  [db-s]
  (format "jdbc:%s:%s%s"
          (:subprotocol db-s)
          (:subname db-s)
          (if (:create spec)
            ";create=true"
            "")))

(def config
  {:datastore (rt/sql-database {:connection-uri (connection-uri spec)})
   :migrations (rt/load-resources "migrations")})

;;(rtr/migrate config)
