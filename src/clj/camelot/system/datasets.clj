(ns camelot.system.datasets
  (:require
   [camelot.system.protocols :as protocols]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]))

;; TODO allow reloading of definitions
(defrecord Datasets [config database migrater ref]
  protocols/Connectable
  (connect [this id]
    (if-let [dataset (-> @ref :definitions id)]
      (do
        (log/info "Connecting to" (name id) "...")
        (try
          (.connect database (get-in dataset [:paths :database]))
          (.migrate migrater dataset)
          (swap! (:ref this) update :available conj id)
          true
          (catch Exception e
            (log/error e)
            false)))
      (do
        (log/error "Dataset not defined")
        false)))


  (disconnect [this id]
    (if-let [dataset (-> @ref :definitions id)]
      (do
        (log/info "Disconnecting from" (name id) "...")
        (try
          (.disconnect database (get-in dataset [:paths :database]))
          (swap! (:ref this) update :available disj id)
          true
          (catch Exception e
            (log/error e)
            false)))
      (do
        (log/error "Dataset not defined")
        false)))

  protocols/Inspectable
  (inspect [this]
    (when-let [ref (:ref this)]
      (:available @ref)))

  component/Lifecycle
  (start [this]
    (log/info config)
    (let [datasets (keys (:datasets config))
          next (assoc this :ref (atom {:definitions (:datasets config)
                                       :available #{}}))]
      (doall (map #(.connect next %) datasets))
      (if (empty? (.inspect next))
        (throw (ex-info "Unable to connect to any Databases" {:tried datasets}))
        next)))

  (stop [this]
    (log/info "Stopping datasets...")
    (doall (map #(.disconnect this %) (.inspect this)))
    (assoc this :ref nil)))
