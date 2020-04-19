(ns camelot.http.sighting
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context DELETE GET POST]]
   [camelot.model.sighting :as sighting]
   [camelot.util.crud :as crud]))

(def routes
  (context "/sightings" {state :state}
           (GET "/media/:id" [id] (crud/list-resources sighting/get-all :sighting id state))
           (GET "/:id" [id] (crud/specific-resource sighting/get-specific id state))
           (GET "/available/:id" [id] (crud/list-available sighting/get-available id state))
           (GET "/alternatives/:id" [id] (crud/list-available sighting/get-alternatives id state))
           (POST "/" [data] (crud/create-resource sighting/create!
                                                  sighting/tsighting data state))
           (DELETE "/media" [data] (r/response (sighting/delete-with-media-ids! state
                                                                                (:media-ids data))))
           (DELETE "/:id" [id] (crud/delete-resource sighting/delete! id state))))


