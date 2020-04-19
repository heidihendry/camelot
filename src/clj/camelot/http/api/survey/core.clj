(ns camelot.http.api.survey.core
  (:require
   [camelot.http.api.survey.spec :as spec]
   [camelot.http.api.survey.resources :as resources]
   [camelot.http.api.spec.core :as api-core]
   [compojure.api.sweet :refer [context DELETE GET POST PATCH resource]]
   [clojure.spec.alpha :as s]))

(def routes
  (context "/surveys" {state :state}
    :tags ["surveys"]

    (GET "/:id" [id]
      :summary "Retrieve a survey with the given ID"
      :return (s/merge
               ::api-core/json-api-without-data
               (s/keys :req-un [::spec/data]))
      (resources/get-with-id state id))

    (GET "/" []
      :summary "Retrieve all surveys"
      :return (s/merge
               ::api-core/json-api-without-data
               (s/keys :req-un [:camelot.http.api.survey.get-all/data]))
      (resources/get-all state))

    (PATCH "/:id" [id]
      :summary "Update a survey"
      :body [data (s/keys :req-un [:camelot.http.api.survey.patch/data])]
      :return (s/merge
               ::api-core/json-api-without-data
               (s/keys :req-un [::spec/data]))
      (resources/patch state id data))

    (POST "/" []
      :summary "Create a new survey"
      :body [data (s/keys :req-un [:camelot.http.api.survey.post/data])]
      :return (s/merge
               ::api-core/json-api-without-data
               (s/keys :req-un [::spec/data]))
      (resources/post state data))

    (DELETE "/:id" [id]
      :summary "Delete a survey"
      (resources/delete state id))))
