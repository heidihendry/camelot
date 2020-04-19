(ns camelot.http.photo
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.photo :as photo]
   [camelot.util.crud :as crud]))

(def routes
  (context "/photos" {state :state}
           (GET "/media/:id" [id] (crud/list-resources photo/get-all :photo id state))
           (GET "/:id" [id] (crud/specific-resource photo/get-specific id state))
           (PUT "/:id" [id data] (crud/update-resource photo/update! id
                                                       photo/tphoto data state))
           (POST "/" [data] (crud/create-resource photo/create!
                                                  photo/tphoto data state))
           (DELETE "/:id" [id] (crud/delete-resource photo/delete! id state))))

