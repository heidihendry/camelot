(ns camelot.routes
  (:require [camelot.application :as app]
            [camelot.handler
             [albums :as albums]
             [application :as application]
             [associated-taxonomy :as ataxonomy]
             [config :as config]
             [capture :as capture]
             [import :as import]
             [bulk-import :as bulk-import]]
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
             [species-mass :as species-mass]
             [survey :as survey]
             [survey-site :as survey-site]
             [survey-file :as survey-file]
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
  (GET "/quit" [] (System/exit 0))
  (route/resources "/"))

(defroutes app-routes
  (context "/trap-stations" {session :session}
           (GET "/site/:id" [id]
                (rest/list-resources trap-station/get-all :trap-station id session))
           (GET "/survey/:id" [id]
                (rest/list-resources trap-station/get-all-for-survey :trap-station id session))
           (GET "/" []
                (rest/list-resources trap-station/get-all* :trap-station session))
           (GET "/:id" [id] (rest/specific-resource trap-station/get-specific id session))
           (PUT "/:id" [id data] (rest/update-resource trap-station/update! id
                                                       trap-station/ttrap-station data session))
           (POST "/" [data] (rest/create-resource trap-station/create!
                                                  trap-station/ttrap-station data session))
           (DELETE "/:id" [id] (rest/delete-resource trap-station/delete! id session)))

  (context "/trap-station-sessions" {session :session}
           (GET "/trap-station/:id" [id]
                (rest/list-resources trap-station-session/get-all :trap-station-session id session))
           (GET "/:id" [id] (rest/specific-resource trap-station-session/get-specific id session))
           (PUT "/:id" [id data] (rest/update-resource trap-station-session/update! id
                                                       trap-station-session/ttrap-station-session data session))
           (POST "/" [data] (rest/create-resource trap-station-session/create!
                                                  trap-station-session/ttrap-station-session data session))
           (DELETE "/:id" [id] (rest/delete-resource trap-station-session/delete! id session)))

  (context "/trap-station-session-cameras" {session :session}
           (GET "/trap-station-session/:id" [id]
                (rest/list-resources trap-station-session-camera/get-all :trap-station-session-camera id session))
           (GET "/available/:id" [id] (rest/list-available trap-station-session-camera/get-available id session))
           (GET "/alternatives/:id" [id] (rest/list-available trap-station-session-camera/get-alternatives id session))
           (GET "/:id" [id] (rest/specific-resource trap-station-session-camera/get-specific id session))
           (PUT "/:id" [id data] (rest/update-resource trap-station-session-camera/update! id
                                                       trap-station-session-camera/ttrap-station-session-camera data session))
           (POST "/" [data] (rest/create-resource trap-station-session-camera/create!
                                                  trap-station-session-camera/ttrap-station-session-camera data session))
           (DELETE "/:id" [id] (rest/delete-resource trap-station-session-camera/delete! id session))
           (DELETE "/:id/media" [id] (rest/delete-resource
                                      trap-station-session-camera/delete-media! id session)))

  (context "/sites" {session :session}
           (GET "/" [] (rest/list-resources site/get-all :site session))
           (GET "/:id" [id] (rest/specific-resource site/get-specific id session))
           (PUT "/:id" [id data] (rest/update-resource site/update! id
                                                       site/tsite data session))
           (POST "/" [data] (rest/create-resource site/create!
                                                  site/tsite data session))
           (DELETE "/:id" [id] (rest/delete-resource site/delete! id session)))

  (context "/cameras" {session :session}
           (GET "/" [] (rest/list-resources camera/get-all :camera session))
           (GET "/available" [] (rest/list-resources camera/get-available :camera session))
           (GET "/:id" [id] (rest/specific-resource camera/get-specific id session))
           (PUT "/:id" [id data] (rest/update-resource camera/update! id
                                                       camera/tcamera data session))
           (POST "/" [data] (rest/create-resource camera/create!
                                                  camera/tcamera data session))
           (DELETE "/:id" [id] (rest/delete-resource camera/delete! id session)))

  (context "/surveys" {session :session}
           (GET "/" [] (rest/list-resources survey/get-all :survey session))
           (GET "/:id/files" [id] (rest/list-resources survey-file/get-all :survey-file id session))
           (GET "/:id/files/:file-id" [id file-id] (rest/specific-resource survey-file/get-specific
                                                                           file-id session))
           (GET "/:id/files/:file-id/download" [id file-id]
                (survey-file/download (app/gen-state (conf/config session))
                                      (edn/read-string file-id)))
           (POST "/files" {params :multipart-params}
                 (r/response (survey-file/upload! (app/gen-state (conf/config session))
                                                  (edn/read-string (get params "survey-id"))
                                                   (get params "file"))))
           (DELETE "/:id/files/:file-id" [id file-id] (rest/delete-resource survey-file/delete!
                                                                            file-id session))
           (GET "/:id" [id] (rest/specific-resource survey/get-specific id session))
           (GET "/bulkimport/template" [] (bulk-import/metadata-template
                                           (app/gen-state (conf/config session))))
           (PUT "/:id" [id data] (rest/update-resource survey/update! id
                                                       survey/tsurvey data session))
           (POST "/" [data] (rest/create-resource survey/create!
                                                  survey/tsurvey data session))
           (DELETE "/:id" [id] (rest/delete-resource survey/delete! id session)))

  (context "/survey-sites" {session :session}
           (GET "/" [] (rest/list-resources survey-site/get-all* :survey-site session))
           (GET "/survey/:id" [id] (rest/list-resources survey-site/get-all :survey-site id session))
           (GET "/:id" [id] (rest/specific-resource survey-site/get-specific id session))
           (GET "/available/:id" [id] (rest/list-available survey-site/get-available id session))
           (GET "/alternatives/:id" [id] (rest/list-available survey-site/get-alternatives id session))
           (PUT "/:id" [id data] (rest/update-resource survey-site/update! id
                                                       survey-site/tsurvey-site data session))
           (POST "/" [data] (rest/create-resource survey-site/create!
                                                  survey-site/tsurvey-site data session))
           (DELETE "/:id" [id] (rest/delete-resource survey-site/delete! id session)))

  (context "/sightings" {session :session}
           (GET "/media/:id" [id] (rest/list-resources sighting/get-all :sighting id session))
           (GET "/:id" [id] (rest/specific-resource sighting/get-specific id session))
           (PUT "/:id" [id data] (rest/update-resource sighting/update! id
                                                       sighting/tsighting data session))
           (GET "/available/:id" [id] (rest/list-available sighting/get-available id session))
           (GET "/alternatives/:id" [id] (rest/list-available sighting/get-alternatives id session))
           (POST "/" [data] (rest/create-resource sighting/create!
                                                  sighting/tsighting data session))
           (DELETE "/:id" [id] (rest/delete-resource sighting/delete! id session)))

  (context "/camera-statuses" {session :session}
           (GET "/available/" [] (rest/list-resources camera-status/get-all :camera-status session))
           (GET "/alternatives/:id" [id] (rest/list-resources camera-status/get-all :camera-status session)))

  (context "/media" {session :session}
           (GET "/trap-station-session-camera/:id" [id] (rest/list-resources media/get-all :media id session))
           (GET "/:id" [id] (rest/specific-resource media/get-specific id session))
           (PUT "/:id" [id data] (rest/update-resource media/update! id media/tmedia data session))
           (POST "/" [data] (rest/create-resource media/create!
                                                  media/tmedia data session))
           (DELETE "/:id" [id] (rest/delete-resource media/delete! id session))
           (GET "/photo/:filename" [filename] (let [style :original]
                                                {:status 200
                                                 :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                 :body (media/read-media-file (app/gen-state (conf/config session))
                                                                              filename (keyword style))}))
           (GET "/photo/:filename/:style" [filename style] {:status 200
                                                            :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                            :body (media/read-media-file (app/gen-state (conf/config session))
                                                                                         filename (keyword style))}))

  (context "/photos" {session :session}
           (GET "/media/:id" [id] (rest/list-resources photo/get-all :photo id session))
           (GET "/:id" [id] (rest/specific-resource photo/get-specific id session))
           (PUT "/:id" [id data] (rest/update-resource photo/update! id
                                                       photo/tphoto data session))
           (POST "/" [data] (rest/create-resource photo/create!
                                                  photo/tphoto data session))
           (DELETE "/:id" [id] (rest/delete-resource photo/delete! id session)))

  (context "/taxonomy" {session :session}
           (GET "/" [] (rest/list-resources taxonomy/get-all :taxonomy session))
           (GET "/available/:id" [id] (rest/list-resources taxonomy/get-all :taxonomy session))
           (GET "/alternatives/:id" [id] (rest/list-resources taxonomy/get-all :taxonomy session))
           (GET "/:id" [id] (rest/specific-resource taxonomy/get-specific id session))
           (PUT "/:id" [id data] (rest/update-resource taxonomy/update! id
                                                       taxonomy/ttaxonomy data session))
           (GET "/survey/:id" [id] (rest/list-resources taxonomy/get-all-for-survey
                                                        :taxonomy id session))
           (DELETE "/:taxonomy-id/survey/:survey-id" [taxonomy-id survey-id]
                   (rest/delete-resource taxonomy/delete-from-survey!
                                         {:survey-id survey-id
                                          :taxonomy-id taxonomy-id} session))
           (POST "/" [data] (rest/create-resource ataxonomy/create!
                                                  ataxonomy/tassociated-taxonomy data session))
           (DELETE "/:id" [id] (rest/delete-resource taxonomy/delete! id session)))

  (context "/species-mass" {session :session}
           (GET "/" [] (rest/list-resources species-mass/get-all :species-mass session))
           (GET "/available/" [id] (rest/list-resources species-mass/get-all :species-mass session))
           (GET "/alternatives/:id" [id] (rest/list-resources species-mass/get-all :species-mass session)))

  (context "/application" {session :session}
           (GET "/metadata" [] (r/response (application/get-metadata (app/gen-state (conf/config session)))))
           (GET "/" [] (r/response {:version (application/get-version)
                                    :nav (application/get-nav-menu (app/gen-state (conf/config session)))})))

  (context "/screens" {session :session}
           (GET "/" [] (r/response (app/all-screens (app/gen-state (conf/config session))))))

  (context "/import" {session :session}
           (POST "/options" [data] (r/response (im.db/options data)))
           (POST "/media" [data] (r/response (import/media data))))

  (context "/report" {session :session}
           (GET "/:report/download" {params :params}
                (report/export
                 (app/gen-state (conf/config session))
                 (keyword (:report params)) (rest/coerce-string-fields params)))
           (GET "/:report" [report] (r/response (report/get-configuration
                                                 (app/gen-state (conf/config session))
                                                 (keyword report))))
           (GET "/" [] (r/response (report/available-reports (app/gen-state (conf/config session))))))

  (context "/library" {session :session}
           (GET "/" [] (r/response (library/build-library (app/gen-state (conf/config session)))))
           (GET "/:id" [id] (r/response (library/build-library-for-survey (app/gen-state (conf/config session))
                                                                          (edn/read-string id))))
           (POST "/media/flags" [data] (r/response (library/update-bulk-media-flags
                                                    (app/gen-state (conf/config session)) data)))
           (PUT "/identify" [data] (r/response (library/identify (app/gen-state (conf/config session))
                                                                 data))))
  (context "/species" {session :session}
           (GET "/search" {{search :search} :params}
                (r/response (species-search/query-search
                             (app/gen-state (conf/config session))
                             {:search search})))
           (POST "/create" [data] (r/response (species-search/ensure-survey-species-known
                                               (app/gen-state (conf/config session))
                                               (:species data)
                                               (:survey-id data)))))

  (context "/capture" {session :session}
           (POST "/upload" {params :multipart-params}
                 (r/response (capture/import-capture! (app/gen-state (conf/config session))
                                                      (edn/read-string (get params "session-camera-id"))
                                                      (get params "file")))))
  (context "/deployment" {session :session}
           (GET "/survey/:id" [id] (rest/list-resources deployment/get-all
                                                        :trap-station-session id session))
           (GET "/survey/:id/recent" [id] (rest/list-resources deployment/get-uploadable
                                                               :trap-station-session id session))
           (GET "/:id" [id] (rest/specific-resource deployment/get-specific id session))
           (POST "/create/:id" [id data] (rest/create-resource deployment/create!
                                                               deployment/tdeployment
                                                               (assoc data :survey-id
                                                                      {:value (edn/read-string id)}) session))
           (PUT "/:id" [id data] (rest/update-resource deployment/update! id
                                                       deployment/tdeployment
                                                       (assoc data :trap-station-id
                                                              {:value (edn/read-string id)}) session))
           (POST "/" [data] (rest/create-resource deployment/create-camera-check!
                                                  deployment/tcamera-deployment data session)))

  misc-routes
  config/routes
  albums/routes)
