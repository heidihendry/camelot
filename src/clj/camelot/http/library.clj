(ns camelot.http.library
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET POST PUT]]
   [camelot.library.core :as library]))

(def routes
  (context "/library" {session :session state :system}
           (POST "/" [data]
                 (r/response (library/query-media (assoc state :session session)
                                                  (:search data))))
           (GET "/metadata" [] (r/response (library/build-library-metadata
                                            (assoc state :session session))))
           (POST "/hydrate" [data]
                 (r/response (library/hydrate-media (assoc state :session session)
                                                    (:media-ids data))))
           (POST "/media/flags" [data] (r/response (library/update-bulk-media-flags
                                                    (assoc state :session session)
                                                    data)))
           (POST "/identify" [data] (r/response (library/identify (assoc state :session session)
                                                                  data)))
           (PUT "/identify/:id" [id data] (r/response (library/update-identification!
                                                       (assoc state :session session)
                                                       id (:identification data))))))


