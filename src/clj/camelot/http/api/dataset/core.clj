(ns camelot.http.api.dataset.core
  (:require
   [camelot.http.api.dataset.resources :as resources]
   [camelot.http.api.spec.core :as api-core]
   [compojure.api.sweet :refer [context POST]]))

(def routes
  (context "/dataset" {state :state}
    :tags ["datasets"]

    (POST "/:id/disconnect" [id]
      :summary "Disconnect from a dataset with the given ID"
      :return ::api-core/json-api-without-data
      (resources/disconnect! state id))

    (POST "/:id/connect" [id]
      :summary "Connect to a dataset with the given ID"
      :return ::api-core/json-api-without-data
      (resources/connect! state id))

    (POST "/reload" []
      :summary "Reload dataset definitions"
      :return ::api-core/json-api-without-data
      (resources/reload! state))))
