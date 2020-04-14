(ns camelot.http.trap-station-session-camera
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]
   [camelot.util.crud :as crud]))

(def routes
  (context "/trap-station-session-cameras" {session :session state :system}
           (GET "/trap-station-session/:id" [id]
                (crud/list-resources trap-station-session-camera/get-all :trap-station-session-camera id (assoc state :session session)))
           (GET "/available/:id" [id] (crud/list-available trap-station-session-camera/get-available id (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-available trap-station-session-camera/get-alternatives id (assoc state :session session)))
           (GET "/camera/:id" [id] (crud/specific-resource trap-station-session-camera/get-camera-usage id
                                                           (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource trap-station-session-camera/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session-camera/update! id
                                                       trap-station-session-camera/ttrap-station-session-camera data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource trap-station-session-camera/create!
                                                  trap-station-session-camera/ttrap-station-session-camera data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session-camera/delete! id (assoc state :session session)))
           (DELETE "/:id/media" [id] (crud/delete-resource
                                      trap-station-session-camera/delete-media! id (assoc state :session session)))))
