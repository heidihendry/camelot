(ns camelot.http.sighting
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context DELETE GET POST]]
   [camelot.model.sighting :as sighting]
   [camelot.util.crud :as crud]))

(def routes
  (context "/sightings" {session :session state :system}
           (GET "/media/:id" [id] (crud/list-resources sighting/get-all :sighting id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource sighting/get-specific id (assoc state :session session)))
           (GET "/available/:id" [id] (crud/list-available sighting/get-available id (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-available sighting/get-alternatives id (assoc state :session session)))
           (POST "/" [data] (crud/create-resource sighting/create!
                                                  sighting/tsighting data (assoc state :session session)))
           (DELETE "/media" [data] (r/response (sighting/delete-with-media-ids! (assoc state :session session)
                                                                                (:media-ids data))))
           (DELETE "/:id" [id] (crud/delete-resource sighting/delete! id (assoc state :session session)))))


