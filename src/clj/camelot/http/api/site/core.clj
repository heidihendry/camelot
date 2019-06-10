(ns camelot.http.api.site.core
  (:require
   [camelot.http.api.site.spec :as spec]
   [camelot.http.api.site.resources :as resources]
   [camelot.http.api.spec.core :as api-core]
   [compojure.api.sweet :refer [context DELETE GET POST PATCH resource]]
   [clojure.spec.alpha :as s]))

(def routes
  (context "/sites" {session :session state :system}
           :tags ["sites"]

    (GET "/:id" [id]
      :summary "Retrieve a site with the given ID"
      :return (s/merge
               ::api-core/json-api-without-data
               (s/keys :req-un [::spec/data]))
      (resources/get-with-id (assoc state :session session) id))

    (GET "/" []
      :summary "Retrieve all sites"
      :return (s/merge
               ::api-core/json-api-without-data
               (s/keys :req-un [:camelot.http.api.site.get-all/data]))
      (resources/get-all (assoc state :session session)))

    (PATCH "/:id" [id]
      :summary "Update a site"
      :body [data (s/keys :req-un [:camelot.http.api.site.patch/data])]
      :return (s/merge
               ::api-core/json-api-without-data
               (s/keys :req-un [::spec/data]))
      (resources/patch! (assoc state :session session) id data))

    (POST "/" []
      :summary "Create a new site"
      :body [data (s/keys :req-un [:camelot.http.api.site.post/data])]
      :return (s/merge
               ::api-core/json-api-without-data
               (s/keys :req-un [::spec/data]))
      (resources/post! (assoc state :session session) data))

    (DELETE "/:id" [id]
      :summary "Delete a site"
      (resources/delete! (assoc state :session session) id))))
