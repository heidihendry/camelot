(ns camelot.http.camera
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.camera :as camera]
   [camelot.util.crud :as crud]))

(def routes
  (context "/cameras" {state :state}
           (GET "/" [] (crud/list-resources camera/get-all :camera state))
           (GET "/available" [] (crud/list-resources camera/get-available :camera state))
           (GET "/:id" [id] (crud/specific-resource camera/get-specific id state))
           (PUT "/:id" [id data] (crud/update-resource camera/update! id
                                                       camera/tcamera data state))
           (POST "/" [data] (crud/create-resource camera/create!
                                                  camera/tcamera data state))
           (DELETE "/:id" [id] (crud/delete-resource camera/delete! id state))))
