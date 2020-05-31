(ns camelot.http.dataset
  (:require
   [camelot.state.datasets :as datasets]
   [compojure.core :refer [context GET POST]]
   [ring.util.response :as r]))

(defn select-dataset
  [state dataset-id]
  (when ((datasets/get-available (:datasets state)) dataset-id)
    {:status 204
     :session {:dataset-id dataset-id}
     :body nil}))

(defn get-datasets
  [state]
  (let [datasets (:datasets state)]
    (map (fn [x] {:dataset-id x
                  :dataset-name (datasets/lookup (datasets/assoc-dataset-context datasets x) :name)})
         (datasets/get-available datasets))))

(def routes
  (context "/dataset" {state :state}
           (GET "/" _
                (r/response {:datasets (get-datasets state)
                             :selected-dataset (datasets/get-dataset-context (:datasets state))}))
           (POST "/select/:id" [id]
                 (select-dataset state (keyword id)))))
