(ns camelot.http.photo
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.photo :as photo]
   [camelot.util.crud :as crud]))

(def routes
  (context "/photos" {session :session state :system}
           (GET "/media/:id" [id] (crud/list-resources photo/get-all :photo id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource photo/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource photo/update! id
                                                       photo/tphoto data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource photo/create!
                                                  photo/tphoto data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource photo/delete! id (assoc state :session session)))))

