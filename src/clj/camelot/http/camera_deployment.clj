(ns camelot.http.camera-deployment
  (:require
   [compojure.core :refer [context GET POST]]
   [camelot.model.camera-deployment :as camera-deployment]
   [camelot.util.crud :as crud]))

(def routes
  (context "/camera-deployment" {session :session state :system}
           (GET "/survey/:id/recent" [id] (crud/list-resources camera-deployment/get-uploadable
                                                               :trap-station-session id (assoc state :session session)))
           (POST "/" [data] (crud/create-resource camera-deployment/create-camera-check!
                                                  camera-deployment/tcamera-deployment data (assoc state :session session)))))

