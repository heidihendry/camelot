(ns camelot.http.camera
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.camera :as camera]
   [camelot.util.crud :as crud]))

(def routes
  (context "/cameras" {session :session state :system}
           (GET "/" [] (crud/list-resources camera/get-all :camera (assoc state :session session)))
           (GET "/available" [] (crud/list-resources camera/get-available :camera (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource camera/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource camera/update! id
                                                       camera/tcamera data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource camera/create!
                                                  camera/tcamera data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource camera/delete! id (assoc state :session session)))))
