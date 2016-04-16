(ns camelot.db
  (:require [ragtime.core :as rtc]
            [ragtime.jdbc :as jdbc]
            [camelot.processing.settings :as settings]))

(def spec {:classname "org.apache.derby.jdbc.EmbeddedDriver",
           :subprotocol "derby",
           :subname (settings/get-db-path),
           :create true})

(def config
  {:datastore (jdbc/sql-database spec)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate
  []
  (rtc/migrate-all (:datastore config)
                   (rtc/into-index (:migrations config))
                   (:migrations config)))
