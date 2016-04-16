(ns camelot.db
  (:require [ragtime.core :as rtc]
            [ragtime.jdbc :as jdbc]))

(def spec {:classname "org.apache.derby.jdbc.EmbeddedDriver",
           :subprotocol "derby",
           :subname "MyDB",
           :create true})

(def config
  {:datastore (jdbc/sql-database spec)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate-db
  []
  (rtc/migrate-all (:datastore config)
                   (rtc/into-index (:migrations config))
                   (:migrations config)))
