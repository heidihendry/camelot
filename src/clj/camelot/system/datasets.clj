(ns camelot.system.datasets
  (:require
   [camelot.system.protocols :as protocols]
   [clojure.tools.logging :as log]
   [slingshot.slingshot :as ss]
   [com.stuartsierra.component :as component]
   [clojure.set :as set]))

(defn- connect* [ref id {:keys [database migrater]}]
  (if-let [dataset (-> @ref :datasets/definitions id)]
    (do
      (log/info "Connecting to" (name id) "...")
      (try
        (.connect database (get-in dataset [:paths :database]))
        (.migrate migrater dataset)
        dataset
        (catch Exception e
          (log/error e)
          (ss/throw+ {:error/type :error.type/internal
                      :error/exception e
                      :entity/type :dataset
                      :entity/id id}))))
    (do
      (log/error "Dataset not defined")
      (ss/throw+ {:error/type :error.type/not-found
                  :entity/type :dataset
                  :entity/id id}))))

(defn- disconnect* [ref id {:keys [database]}]
  (if-let [dataset (-> @ref :datasets/definitions id)]
    (do
      (log/info "Disconnecting from" (name id) "...")
      (try
        (.disconnect database (get-in dataset [:paths :database]))
        dataset
        (catch Exception e
          (log/error e)
          (ss/throw+ {:error/type :error.type/internal
                      :error/exception e
                      :entity/type :dataset
                      :entity/id id}))))
    (do
      (log/error "Dataset not defined")
      (ss/throw+ {:error/type :error.type/not-found
                  :entity/type :dataset
                  :entity/id id}))))

(defrecord Datasets [config database migrater ref]
  protocols/Connectable
  (connect [this id]
    (let [opts {:database database
                :migrater migrater}]
      (when (connect* ref id opts)
        (swap! (:ref this) update :datasets/available conj id)
        true)
      false))

  (disconnect [this id]
    (let [opts {:database database}]
      (when (disconnect* ref id opts)
        (swap! (:ref this) update :datasets/available disj id))))

  protocols/Inspectable
  (inspect [this]
    (when-let [ref (:ref this)]
      @ref))

  protocols/Contextual
  (set-context [this k dataset-id]
    (assoc-in this [::context k] dataset-id))

  (context [this k]
    (get-in this [::context k]))

  protocols/Reloadable
  (reload [this]
    (let [datasets (:datasets (.config config))
          cur-available-datasets (:datasets/available (.inspect this))
          next-available-datasets (set (keys datasets))
          removed-datasets (set/difference cur-available-datasets next-available-datasets)]
      (doseq [dataset removed-datasets]
        (.disconnect this dataset))
      (swap! (:ref this) #(assoc % :datasets/definitions datasets))
      this))

  component/Lifecycle
  (start [this]
    (log/info config)
    (let [dataset-ids (keys (:datasets config))
          next (assoc this :ref (atom {:datasets/definitions (:datasets config)
                                       :datasets/available #{}}))]
      (doseq [id dataset-ids]
        (ss/try+
         (.connect next id)
         (catch Object _
           ;; ignored
           nil)))
      (if (empty? (:datasets/available (.inspect next)))
        (throw (ex-info "Unable to connect to any Databases" {:tried dataset-ids}))
        next)))

  (stop [this]
    (log/info "Stopping datasets...")
    (doall (map #(.disconnect this %) (:datasets/available (.inspect this))))
    (assoc this :ref nil)))
