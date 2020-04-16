(ns camelot.system.http.dataset
  "Ring middleware for dataset selection."
  (:require
   [camelot.util.state :as state]))

(defn- inject-dataset-to-request
  [dataset-id request]
  (assoc-in request [:session :dataset-id] dataset-id))

(defn- inject-dataset-to-response
  [dataset-id response]
  ;; A route *may* explicitly override the dataset, even if defaulted.
  (update-in response [:session :dataset-id] #(or % dataset-id)))

(defn- select-default-dataset
  [request]
  (if-let [dataset-id (first (state/get-dataset-ids (:system request)))]
    dataset-id
    (throw (ex-info "No datasets were found" {}))))

(defn- scoped-to-dataset?
  [request]
  (boolean (get-in request [:session :dataset-id])))

(defn- scope-to-default-dataset
  [handler request]
  (let [dataset-id (select-default-dataset request)]
    (->> request
         (inject-dataset-to-request dataset-id)
         handler
         (inject-dataset-to-response dataset-id))))

(defn wrap-dataset-selection
  "Dataset selection."
  [handler]
  (fn [request]
    (if (scoped-to-dataset? request)
      (handler request)
      (scope-to-default-dataset handler request))))
