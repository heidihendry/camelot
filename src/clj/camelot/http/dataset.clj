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

(def routes
  (context "/dataset" {state :state}
           (GET "/" _
                (r/response {:dataset-ids (state/get-dataset-ids state)
                             :selected-dataset (state/get-dataset-id state)}))
           (POST "/select/:id" [id]
                 (select-dataset state (keyword id)))))
