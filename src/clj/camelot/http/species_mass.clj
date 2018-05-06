(ns camelot.http.species-mass
  (:require
   [compojure.core :refer [context GET]]
   [camelot.model.species-mass :as species-mass]
   [camelot.util.crud :as crud]))

(def routes
  (context "/species-mass" {session :session state :system}
           (GET "/" [] (crud/list-resources species-mass/get-all :species-mass (assoc state :session session)))
           (GET "/available/" [id] (crud/list-resources species-mass/get-all :species-mass (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-resources species-mass/get-all :species-mass (assoc state :session session)))))


