(ns camelot.http.trap-station
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.trap-station :as trap-station]
   [camelot.util.crud :as crud]))

(def routes
  (context "/trap-stations" {session :session state :system}
           (GET "/site/:id" [id]
                (crud/list-resources trap-station/get-all :trap-station id (assoc state :session session)))
           (GET "/survey/:id" [id]
                (crud/list-resources trap-station/get-all-for-survey :trap-station id (assoc state :session session)))
           (GET "/" []
                (crud/list-resources trap-station/get-all* :trap-station (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource trap-station/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource trap-station/update! id
                                                       trap-station/ttrap-station data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource trap-station/create!
                                                  trap-station/ttrap-station data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource trap-station/delete! id (assoc state :session session)))))
