(ns camelot.http.trap-station-session
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.util.crud :as crud]))

(def routes
  (context "/trap-station-sessions" {state :state}
           (GET "/trap-station/:id" [id]
                (crud/list-resources trap-station-session/get-all :trap-station-session id state))
           (GET "/:id" [id] (crud/specific-resource trap-station-session/get-specific id state))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session/update! id
                                                       trap-station-session/ttrap-station-session data state))
           (POST "/" [data] (crud/create-resource trap-station-session/create!
                                                  trap-station-session/ttrap-station-session data state))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session/delete! id state))))
