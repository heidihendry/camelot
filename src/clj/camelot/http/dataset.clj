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
  (context "/dataset" {session :session state :system}
           (GET "/" _
                (r/response {:dataset-ids (state/get-dataset-ids (assoc state :session session))}))
           (POST "/select/:id" [id]
                 (select-dataset (assoc state :session session) (keyword id)))))
