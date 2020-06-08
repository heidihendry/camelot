(ns camelot.http.api.core
  (:require
   [compojure.api.sweet :refer [api context]]
   [camelot.http.api.survey.core :as survey]
   [camelot.http.api.site.core :as site]
   [camelot.http.api.dataset.core :as dataset]))

(def routes
  (context "/api/v1" []
    :coercion :spec
    dataset/routes
    survey/routes
    site/routes))

(def core-api
  (api
   {:swagger
    {:ui "/api"
     :spec "/api/v1/swagger.json"
     :data {:info {:title "Camelot API"
                   :description "RESTful API for Camelot"}}}}
   routes))
