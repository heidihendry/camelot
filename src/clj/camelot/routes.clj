(ns camelot.routes
  (:require [camelot.application :as app]
            [camelot.handler
             [albums :as albums]
             [application :as application]
             [config :as config]
             [capture :as capture]
             [import :as import]]
            [camelot.import.db :as im.db]
            [camelot.services.species-search :as species-search]
            [camelot.model
             [camera :as camera]
             [camera-status :as camera-status]
             [media :as media]
             [photo :as photo]
             [sighting :as sighting]
             [site :as site]
             [taxonomy :as taxonomy]
             [survey :as survey]
             [survey-site :as survey-site]
             [trap-station :as trap-station]
             [trap-station-session :as trap-station-session]
             [trap-station-session-camera :as trap-station-session-camera]
             [deployment :as deployment]
             [library :as library]]
            [camelot.report.core :as report]
            [camelot.util
             [config :as conf]
             [rest :as rest]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [compojure
             [core :refer [context defroutes DELETE GET POST PUT routes]]
             [route :as route]]
            [ring.util.response :as r]))

(defn- retrieve-index
  "Return a response for index.html"
  []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "public/index.html"))})

(defroutes misc-routes
  "Miscellaneous application routes."
  (GET "/" _ (retrieve-index))
  (POST "/quit" [] (System/exit 0))
  (route/resources "/"))

(defroutes app-routes
  (context "/trap-stations" []
           (GET "/site/:id" [id]
                (rest/list-resources trap-station/get-all :trap-station id))
           (GET "/survey/:id" [id]
                (rest/list-resources trap-station/get-all-for-survey :trap-station id))
           (GET "/" []
                (rest/list-resources trap-station/get-all* :trap-station))
           (GET "/:id" [id] (rest/specific-resource trap-station/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource trap-station/update! id
                                                       trap-station/ttrap-station data))
           (POST "/" [data] (rest/create-resource trap-station/create!
                                                  trap-station/ttrap-station data))
           (DELETE "/:id" [id] (rest/delete-resource trap-station/delete! id)))

  (context "/trap-station-sessions" []
           (GET "/trap-station/:id" [id]
                (rest/list-resources trap-station-session/get-all :trap-station-session id))
           (GET "/:id" [id] (rest/specific-resource trap-station-session/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource trap-station-session/update! id
                                                       trap-station-session/ttrap-station-session data))
           (POST "/" [data] (rest/create-resource trap-station-session/create!
                                                  trap-station-session/ttrap-station-session data))
           (DELETE "/:id" [id] (rest/delete-resource trap-station-session/delete! id)))

  (context "/trap-station-session-cameras" []
           (GET "/trap-station-session/:id" [id]
                (rest/list-resources trap-station-session-camera/get-all :trap-station-session-camera id))
           (GET "/available/:id" [id] (rest/list-available trap-station-session-camera/get-available id))
           (GET "/alternatives/:id" [id] (rest/list-available trap-station-session-camera/get-alternatives id))
           (GET "/:id" [id] (rest/specific-resource trap-station-session-camera/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource trap-station-session-camera/update! id
                                                       trap-station-session-camera/ttrap-station-session-camera data))
           (POST "/" [data] (rest/create-resource trap-station-session-camera/create!
                                                  trap-station-session-camera/ttrap-station-session-camera data))
           (DELETE "/:id" [id] (rest/delete-resource trap-station-session-camera/delete! id)))

  (context "/sites" []
           (GET "/" [] (rest/list-resources site/get-all :site))
           (GET "/:id" [id] (rest/specific-resource site/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource site/update! id
                                                       site/tsite data))
           (POST "/" [data] (rest/create-resource site/create!
                                                  site/tsite data))
           (DELETE "/:id" [id] (rest/delete-resource site/delete! id)))

  (context "/cameras" []
           (GET "/" [] (rest/list-resources camera/get-all :camera))
           (GET "/available" [] (rest/list-resources camera/get-available :camera))
           (GET "/:id" [id] (rest/specific-resource camera/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource camera/update! id
                                                       camera/tcamera data))
           (POST "/" [data] (rest/create-resource camera/create!
                                                  camera/tcamera data))
           (DELETE "/:id" [id] (rest/delete-resource camera/delete! id)))

  (context "/surveys" []
           (GET "/" [] (rest/list-resources survey/get-all :survey))
           (GET "/:id" [id] (rest/specific-resource survey/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource survey/update! id
                                                       survey/tsurvey data))
           (POST "/" [data] (rest/create-resource survey/create!
                                                  survey/tsurvey data))
           (DELETE "/:id" [id] (rest/delete-resource survey/delete! id)))

  (context "/survey-sites" []
           (GET "/survey/:id" [id] (rest/list-resources survey-site/get-all :survey-site id))
           (GET "/:id" [id] (rest/specific-resource survey-site/get-specific id))
           (GET "/available/:id" [id] (rest/list-available survey-site/get-available id))
           (GET "/alternatives/:id" [id] (rest/list-available survey-site/get-alternatives id))
           (PUT "/:id" [id data] (rest/update-resource survey-site/update! id
                                                       survey-site/tsurvey-site data))
           (POST "/" [data] (rest/create-resource survey-site/create!
                                                  survey-site/tsurvey-site data))
           (DELETE "/:id" [id] (rest/delete-resource survey-site/delete! id)))

  (context "/sightings" []
           (GET "/media/:id" [id] (rest/list-resources sighting/get-all :sighting id))
           (GET "/:id" [id] (rest/specific-resource sighting/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource sighting/update! id
                                                       sighting/tsighting data))
           (GET "/available/:id" [id] (rest/list-available sighting/get-available id))
           (GET "/alternatives/:id" [id] (rest/list-available sighting/get-alternatives id))
           (POST "/" [data] (rest/create-resource sighting/create!
                                                  sighting/tsighting data))
           (DELETE "/:id" [id] (rest/delete-resource sighting/delete! id)))

  (context "/camera-statuses" []
           (GET "/available/" [] (rest/list-resources camera-status/get-all :camera-status))
           (GET "/alternatives/:id" [id] (rest/list-resources camera-status/get-all :camera-status)))

  (context "/media" []
           (GET "/trap-station-session-camera/:id" [id] (rest/list-resources media/get-all :media id))
           (GET "/:id" [id] (rest/specific-resource media/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource media/update! id media/tmedia data))
           (POST "/" [data] (rest/create-resource media/create!
                                                  media/tmedia data))
           (DELETE "/:id" [id] (rest/delete-resource media/delete! id))
           (GET "/photo/:filename" [filename] (let [style :original]
                                                {:status 200
                                                 :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                 :body (media/read-media-file (app/gen-state (conf/config))
                                                                              filename (keyword style))}))
           (GET "/photo/:filename/:style" [filename style] {:status 200
                                                            :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                            :body (media/read-media-file (app/gen-state (conf/config))
                                                                                         filename (keyword style))}))

  (context "/photos" []
           (GET "/media/:id" [id] (rest/list-resources photo/get-all :photo id))
           (GET "/:id" [id] (rest/specific-resource photo/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource photo/update! id
                                                       photo/tphoto data))
           (POST "/" [data] (rest/create-resource photo/create!
                                                  photo/tphoto data))
           (DELETE "/:id" [id] (rest/delete-resource photo/delete! id)))

  (context "/taxonomy" []
           (GET "/" [] (rest/list-resources taxonomy/get-all :taxonomy))
           (GET "/:id" [id] (rest/specific-resource taxonomy/get-specific id))
           (PUT "/:id" [id data] (rest/update-resource taxonomy/update! id
                                                       taxonomy/ttaxonomy data))
           (POST "/" [data] (rest/create-resource taxonomy/create!
                                                  taxonomy/ttaxonomy data))
           (DELETE "/:id" [id] (rest/delete-resource taxonomy/delete! id)))

  (context "/application" []
           (GET "/metadata" [] (r/response (application/get-metadata (app/gen-state (conf/config)))))
           (GET "/" [] (r/response {:version (application/get-version)
                                    :nav (application/get-nav-menu (app/gen-state (conf/config)))})))

  (context "/screens" []
           (GET "/" [] (r/response (app/all-screens (app/gen-state (conf/config))))))

  (context "/import" []
           (POST "/options" [data] (r/response (im.db/options data)))
           (POST "/media" [data] (r/response (import/media data))))

  (context "/report" []
           (GET "/:report/:id" [report id] (report/export (keyword report) (edn/read-string id)))
           (GET "/:report" [report] (r/response (report/get-configuration (keyword report))))
           (GET "/" [] (r/response (report/available-reports))))

  (context "/library" []
           (GET "/" [] (r/response (library/build-library (app/gen-state (conf/config)))))
           (GET "/:id" [id] (r/response (library/build-library-for-survey (app/gen-state (conf/config))
                                                                          (edn/read-string id))))
           (POST "/media/flags" [data] (r/response (library/update-bulk-media-flags
                                                    (app/gen-state (conf/config)) data)))
           (PUT "/identify" [data] (r/response (library/identify (app/gen-state (conf/config))
                                                                 data))))
  (context "/species" []
           (GET "/search" {{search :search} :params}
                (r/response (species-search/query-search
                             (app/gen-state (conf/config))
                             {:search search})))
           (POST "/create" [data] (r/response (species-search/ensure-survey-species-known
                                               (app/gen-state (conf/config))
                                               (:species data)))))

  (context "/capture" []
           (POST "/upload" {params :multipart-params}
                 (r/response (capture/import-capture! (app/gen-state (conf/config))
                                                      (edn/read-string (get params "session-camera-id"))
                                                      (get params "file")))))
  (context "/deployment" []
           (GET "/survey/:id" [id] (rest/list-resources deployment/get-all
                                                        :trap-station-session id))
           (GET "/survey/:id/recent" [id] (rest/list-resources deployment/get-awaiting-upload
                                                                   :trap-station-session id))
           (GET "/:id" [id] (rest/specific-resource deployment/get-specific id))
           (POST "/create/:id" [id data] (rest/create-resource deployment/create!
                                                               deployment/tdeployment
                                                               (assoc data :survey-id
                                                                      {:value (edn/read-string id)})))
           (POST "/" [data] (rest/create-resource deployment/create-camera-check!
                                                  deployment/tcamera-deployment data)))

  misc-routes
  config/routes
  albums/routes)
