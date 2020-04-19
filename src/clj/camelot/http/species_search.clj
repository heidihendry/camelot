(ns camelot.http.species-search
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET POST]]
   [camelot.services.species-search :as species-search]))

(def routes
  (context "/species" {state :state}
           (GET "/search" {{search :search} :params}
                (r/response (species-search/query-search
                             state
                             {:search search})))
           (POST "/create" [data] (r/response (species-search/ensure-survey-species-known
                                               state
                                               (:species data)
                                               (:survey-id data))))))
