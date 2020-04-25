(ns camelot.http.dataset
  (:require
   [camelot.util.state :as state]
   [compojure.core :refer [context GET POST]]
   [ring.util.response :as r]))

(defn select-dataset
  [state dataset-id]
  (when (state/get-dataset state dataset-id)
    {:status 204
     :session {:dataset-id dataset-id}
     :body nil}))

(defn get-datasets
  [state]
  (map (fn [x] {:dataset-id x
                :dataset-name (:name (state/get-dataset state x))})
   (state/get-dataset-ids state)))

(def routes
  (context "/dataset" {state :state}
           (GET "/" _
                (r/response {:datasets (get-datasets state)
                             :selected-dataset (state/get-dataset-id state)}))
           (POST "/select/:id" [id]
                 (select-dataset state (keyword id)))))
