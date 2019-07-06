(ns camelot.library.query.core
  (:require [camelot.library.query.translate :as translate]
            [camelot.library.query.honeysql :as honeysql]
            [camelot.library.query.fields :as fields]
            [camelot.library.query.sighting-fields :as sighting-fields]
            [camelot.library.query.util :as qutil]
            [camelot.util.db :as db]
            [bitpattern.simql.core :as simql-parser]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

(def ^:private sqlquery (db/with-db-keys :library))

(defn- optimised-query
  [state pt]
  (cond
    (qutil/match-all? pt)
    (map :media-id (sqlquery state :all-media-ids {}))

    (qutil/match-all-in-survey? pt)
    (map :media-id (sqlquery state :all-media-ids-for-survey
                             {:field-value (qutil/first-integer-value pt)}))))

(defn- full-query
  [state pt]
  (->> pt
       translate/parse->sql
       honeysql/build-query
       (sighting-fields/join-fields pt)
       honeysql/finalise-query
       (jdbc/query (get-in state [:database :connection]))
       (map :media_id)))

(defn create-parser
  [state]
  (simql-parser/create-parser {:fields (fields/field-datatypes state)}))

(defn query-media
  [state qstr]
  (try
    (let [parse (create-parser state)
          pt (parse qstr)]
      (or (optimised-query state pt)
          (full-query state pt)))
    (catch Exception e
      (log/error (.getMessage e))
      (doseq [st (.getStackTrace e)]
        (log/error (.toString st)))
      '())))
