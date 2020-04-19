(ns camelot.http.camera-deployment
  (:require
   [compojure.core :refer [context GET POST]]
   [camelot.model.camera-deployment :as camera-deployment]
   [camelot.util.crud :as crud]))

(def routes
  (context "/camera-deployment" {state :state}
           (GET "/survey/:id/recent" [id] (crud/list-resources camera-deployment/get-uploadable
                                                               :trap-station-session id state))
           (POST "/" [data] (crud/create-resource camera-deployment/create-camera-check!
                                                  camera-deployment/tcamera-deployment data state))))

