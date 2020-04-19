(ns camelot.http.species-mass
  (:require
   [compojure.core :refer [context GET]]
   [camelot.model.species-mass :as species-mass]
   [camelot.util.crud :as crud]))

(def routes
  (context "/species-mass" {state :state}
           (GET "/" [] (crud/list-resources species-mass/get-all :species-mass state))
           (GET "/available/" [id] (crud/list-resources species-mass/get-all :species-mass state))
           (GET "/alternatives/:id" [id] (crud/list-resources species-mass/get-all :species-mass state))))


