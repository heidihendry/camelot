(ns camelot.handler.media
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [clojure.java.io :as io]
            [camelot.util.config :as config]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.util.java-file :as jf]
            [clojure.string :as str]))

(sql/defqueries "sql/media.sql" {:connection db/spec})

(s/defn get-all
  [state id]
  (db/with-db-keys state -get-all {:trap-station-session-camera-id id}))

(s/defn get-specific
  [state
   id :- s/Num]
  (first (db/with-db-keys state -get-specific {:media-id id})))

(s/defn create!
  [state data]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   id :- s/Num
   data]
  (db/with-db-keys state -update! (merge data {:media-id id}))
  (get-specific state (:media-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys state -delete! {:media-id id}))

(defn read-media-file
  [filename]
  (let [f (io/file (str (config/get-media-path) "/" filename))]
    (io/input-stream
     (if (jf/readable? f)
       f
       (io/file (str (config/get-media-path) "/" (str/lower-case filename)))))))

(def routes
  (context "/media" []
           (GET "/trap-station-session-camera/:id" [id] (rest/list-resources get-all :media id))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))
           (GET "/photo/:filename" [filename] {:status 200
                                               :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                               :body (read-media-file filename)})))
