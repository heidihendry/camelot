(ns camelot.http.deployment
  (:require
   [clojure.edn :as edn]
   [compojure.core :refer [context GET POST PUT]]
   [camelot.model.deployment :as deployment]
   [camelot.util.crud :as crud]))

(def routes
  (context "/deployment" {state :state}
           (GET "/survey/:id" [id] (crud/list-resources deployment/get-all
                                                        :trap-station-session id state))
           (POST "/create/:id" [id data] (crud/create-resource deployment/create!
                                                               deployment/tdeployment
                                                               (assoc data :survey-id
                                                                      {:value (edn/read-string id)}) state))
           (PUT "/:id" [id data] (crud/update-resource deployment/update! id
                                                       deployment/tdeployment
                                                       (assoc data :trap-station-id
                                                              {:value (edn/read-string id)}) state))
           (GET "/:id" [id] (crud/specific-resource deployment/get-specific id state))))
