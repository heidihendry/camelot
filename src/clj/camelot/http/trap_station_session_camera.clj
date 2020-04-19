(ns camelot.http.trap-station-session-camera
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]
   [camelot.util.crud :as crud]))

(def routes
  (context "/trap-station-session-cameras" {state :state}
           (GET "/trap-station-session/:id" [id]
                (crud/list-resources trap-station-session-camera/get-all :trap-station-session-camera id state))
           (GET "/available/:id" [id] (crud/list-available trap-station-session-camera/get-available id state))
           (GET "/alternatives/:id" [id] (crud/list-available trap-station-session-camera/get-alternatives id state))
           (GET "/camera/:id" [id] (crud/specific-resource trap-station-session-camera/get-camera-usage id
                                                           state))
           (GET "/:id" [id] (crud/specific-resource trap-station-session-camera/get-specific id state))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session-camera/update! id
                                                       trap-station-session-camera/ttrap-station-session-camera data state))
           (POST "/" [data] (crud/create-resource trap-station-session-camera/create!
                                                  trap-station-session-camera/ttrap-station-session-camera data state))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session-camera/delete! id state))
           (DELETE "/:id/media" [id] (crud/delete-resource
                                      trap-station-session-camera/delete-media! id state))))
