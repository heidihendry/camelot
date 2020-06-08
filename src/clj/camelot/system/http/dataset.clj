(ns camelot.system.http.dataset
  "Ring middleware for dataset selection."
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [camelot.state.datasets :as datasets]))

(defn assoc-context
  [request dataset-id]
  (update-in request [:system :datasets] datasets/assoc-dataset-context dataset-id))

(defn- inject-dataset-to-response
  [response dataset-id]
  ;; A route *may* explicitly override the dataset, even if defaulted.
  (update-in response [:session :dataset-id] #(or % dataset-id)))

(defn- select-default-dataset
  [request]
  (when-let [dataset-id (first (datasets/get-available (:datasets (:system request))))]
    dataset-id))

(defn- requested-dataset-scope
  [request]
  (let [dataset-id (get-in request [:session :dataset-id])
        available-datasets (datasets/get-available (:datasets (:system request)))]
    (when (and (boolean dataset-id)
               (available-datasets dataset-id))
      dataset-id)))

(defn- scope-to-default-dataset
  [handler request]
  (if-let [dataset-id (select-default-dataset request)]
    (-> request
         (assoc-context dataset-id)
         handler
         (inject-dataset-to-response dataset-id))
    (handler request)))

(defn wrap-dataset-selection
  "Dataset selection."
  [handler]
  (fn [request]
    (if (str/starts-with? (:uri request) "/api")
      (handler request)
      (if-let [dataset-id (requested-dataset-scope request)]
        (handler (assoc-context request dataset-id))
        (scope-to-default-dataset handler request)))))
