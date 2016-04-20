(ns camelot.handler.surveys
  (:require [camelot.util.java-file :as jf]
            [camelot.db :as db]
            [yesql.core :as sql]
            [clojure.java.io :as f]))

;;(sql/defqueries "surveys.sql" {:connection db/spec})
;;
;;(defn- check-directory
;;  [state sdir]
;;  (let [dh (f/file sdir)]
;;    (cond
;;      (not (jf/exists? dh)) ((:translate state) :problems/path-not-found sdir)
;;      (not (jf/directory? dh)) ((:translate state) :problems/not-directory sdir)
;;      (not (jf/readable? dh)) ((:translate state) :problems/read-permission-denied sdir)
;;      :else nil)))
;;
;;(defn create!
;;  [state sname sdir]
;;  {:pre [(not (nil? sname))]}
;;  ;; TODO handle attempt to add duplicate name to table
;;  (let [err (check-directory state sdir)]
;;    (if err
;;      err
;;      (-create<! {:name sname :directory sdir}))))
;;
;;(defn get-specific
;;  [state sid]
;;  (-get-specific {:id sid}))
;;
;;(defn get-all
;;  [state]
;;  (-get-all))
;;
;;(defn update!
;;  [state sid sname sdir]
;;  (let [err (check-directory state sdir)]
;;    (if err
;;      err
;;      (-update! {:id sid :name sname :directory sdir}))))
;;
;;(defn delete!
;;  [state id]
;;  (-delete! id))
