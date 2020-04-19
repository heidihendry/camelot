(ns camelot.http.library
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET POST PUT]]
   [camelot.library.core :as library]))

(def routes
  (context "/library" {state :state}
           (POST "/" [data]
                 (r/response (library/query-media state
                                                  (:search data))))
           (GET "/metadata" [] (r/response (library/build-library-metadata
                                            state)))
           (POST "/hydrate" [data]
                 (r/response (library/hydrate-media state
                                                    (:media-ids data))))
           (POST "/media/flags" [data] (r/response (library/update-bulk-media-flags
                                                    state
                                                    data)))
           (POST "/identify" [data] (r/response (library/identify state
                                                                  data)))
           (PUT "/identify/:id" [id data] (r/response (library/update-identification!
                                                       state
                                                       id (:identification data))))))


