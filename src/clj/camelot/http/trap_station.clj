(ns camelot.http.trap-station
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.trap-station :as trap-station]
   [camelot.util.crud :as crud]))

(def routes
  (context "/trap-stations" {state :state}
           (GET "/site/:id" [id]
                (crud/list-resources trap-station/get-all :trap-station id state))
           (GET "/survey/:id" [id]
                (crud/list-resources trap-station/get-all-for-survey :trap-station id state))
           (GET "/" []
                (crud/list-resources trap-station/get-all* :trap-station state))
           (GET "/:id" [id] (crud/specific-resource trap-station/get-specific id state))
           (PUT "/:id" [id data] (crud/update-resource trap-station/update! id
                                                       trap-station/ttrap-station data state))
           (POST "/" [data] (crud/create-resource trap-station/create!
                                                  trap-station/ttrap-station data state))
           (DELETE "/:id" [id] (crud/delete-resource trap-station/delete! id state))))
