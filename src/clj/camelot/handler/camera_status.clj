(ns camelot.handler.camera-status
  (:require [camelot.util.java-file :as jf]
            [camelot.db :as db]
            [yesql.core :as sql]
            [clojure.java.io :as f]))

(sql/defqueries "sql/camera-status.sql" {:connection db/spec})

(defn get-all
  [state]
  (-get-all))
