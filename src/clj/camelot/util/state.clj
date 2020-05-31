(ns camelot.util.state
  (:require
   [camelot.state.datasets :as datasets]
   [camelot.state.database :as database]))

;; TODO #217 remove from this ns
(defn get-dataset-ids
  [state]
  (-> state :config :dataset-ids))

;; TODO #217 remove from this ns
(defn get-dataset
  [state dataset-id]
  (if-let [dataset (get-in state [:config :datasets dataset-id])]
    dataset
    (throw (ex-info "Dataset not found" {:dataset-id dataset-id}))))

;; TODO #217 remove from this ns
(defn get-dataset-id [state]
  (if-let [dataset-id (get-in state [:session :dataset-id])]
    dataset-id
    (throw (ex-info "Dataset ID not set" {}))))

;; TODO #217 remove dataset concern from this ns
(defn lookup [state k]
  (let [dataset-id (get-dataset-id state)
        dataset (get-dataset state dataset-id)]
    (get (merge (:config state) dataset (or (:session state) {})) k)))

;; TODO #217 move these to some suitable database'y namespace
(defn spec-for-dataset
  "JDBC spec for a dataset."
  [dataset]
  (database/spec (-> dataset :paths :database)))

(defn spec
  "JDBC spec for the database."
  [state]
  ;; TODO #217 remove usage of lookup-path / move this call
  (database/spec (datasets/lookup-path (:datasets state) :database)))

;; TODO #217 kill this in favour of `spec`
(defn lookup-connection [state]
  (if (get-dataset-id state)
    (spec state)
    (throw (ex-info "Database connection not found" {}))))

(defn with-dataset
  "Return a copy of `state` with the `dataset-id` set."
  [state dataset-id]
  (if-let [current-dataset-id (get-in state [:session :dataset-id])]
    (throw (ex-info "Dataset ID already set" {:current-dataset-id current-dataset-id
                                              :requested-dataset-id dataset-id}))
    (assoc-in state [:session :dataset-id] dataset-id)))

(defn map-datasets
  [f system-state]
  (map #(let [state (with-dataset system-state %)]
          (f state))
       (get-dataset-ids system-state)))

(defn dissoc-dataset
  [state]
  (update state :session dissoc :dataset-id))
