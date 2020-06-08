(ns camelot.state.datasets
  (:require
   [camelot.state.database :as database]))

(defn assoc-dataset-context
  [datasets dataset-id]
  (let [available (:datasets/available (.inspect datasets))]
    (if (dataset-id available)
      (.set-context datasets ::dataset dataset-id)
      (throw (ex-info "Dataset not found" {:dataset dataset-id
                                           :available available})))))

(defn get-dataset-context
  [datasets]
  (.context datasets ::dataset))

(defn assoc-connection-context
  [datasets connection]
  (.set-context datasets ::connection connection))

(defn get-connection-context
  [datasets]
  (.context datasets ::connection))

(defn get-available
  [datasets]
  (:datasets/available (.inspect datasets)))

(defn lookup
  [datasets k]
  (if-let [ctx (.context datasets ::dataset)]
    (get-in (:datasets/definitions (.inspect datasets)) [ctx :paths k])
    (throw (ex-info "Dataset context not established" {}))))

(defn lookup-path
  [datasets k]
  (if-let [ctx (.context datasets ::dataset)]
    (get-in (:datasets/definitions (.inspect datasets)) [ctx :paths k])
    (throw (ex-info "Dataset context not established" {}))))

(defn lookup-connection
  [datasets]
  (if (get-dataset-context datasets)
    (or (get-connection-context datasets)
        (database/spec (lookup-path datasets :database)))
    (throw (ex-info "Database connection not found" {:datasets datasets}))))

(defn connect!
  [datasets id]
  (.connect datasets id))

(defn disconnect!
  [datasets id]
  (.disconnect datasets id))

(defn reload!
  [datasets]
  (let [ctx (get-dataset-context datasets)
        next (.reload datasets)
        available (get-available next)]
    (when (and ctx (not (available ctx)))
      (throw (ex-info "Currently selected dataset was disconnected during reload"
                      {:context ctx
                       :datasets available})))
    next))
