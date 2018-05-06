(ns camelot.http.trap-station-session
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.util.crud :as crud]))

(def routes
  (context "/trap-station-sessions" {session :session state :system}
           (GET "/trap-station/:id" [id]
                (crud/list-resources trap-station-session/get-all :trap-station-session id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource trap-station-session/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session/update! id
                                                       trap-station-session/ttrap-station-session data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource trap-station-session/create!
                                                  trap-station-session/ttrap-station-session data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session/delete! id (assoc state :session session)))))
