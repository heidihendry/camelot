(ns camelot.state.database
  (:require
   [clojure.java.io :as io])
  (:import
   (java.io File)))

(defn spec
  "JDBC spec for the database."
  [^File database]
  {:classname "org.apache.derby.jdbc.EmbeddedDriver",
   :subprotocol "derby",
   :subname (io/file (.getPath database)
                     ;; TODO this should be in camelot market
                     "Database"),
   :create true})
