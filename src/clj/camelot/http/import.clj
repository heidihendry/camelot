(ns camelot.http.import
  (:require
   [clojure.edn :as edn]
   [ring.util.response :as r]
   [compojure.core :refer [context GET POST]]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [camelot.import.core :as import]
   [camelot.import.template :as template]
   [camelot.import.bulk :as bulk]))

(def time-formatter (tf/formatter-local "yyyy-MM-dd_HHmm"))

(defn- content-disposition
  []
  (format "attachment; filename=\"bulk-import-template_%s.csv\""
          (tf/unparse time-formatter (tl/local-now))))

(defn metadata-template
  "Respond with the template as a CSV."
  [state client-dir]
  (let [data (template/generate-template state client-dir)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition"
                  (content-disposition)))))

(def routes
  (context "/import" {session :session state :system}
           (GET "/import/bulk/template" {params :params}
                (metadata-template (assoc state :session session) (:dir params)))
           (POST "/import/bulk/columnmap" {params :multipart-params}
                 (->> (get params "file")
                      (template/column-map-options (assoc state :session session))
                      r/response))
           (POST "/import/bulk/import" [data]
                 (r/response (bulk/import-with-mappings (assoc state :session session) data)))
           (GET "/" [] (r/response (import/importer-state state)))
           (POST "/cancel" [] (r/response (import/cancel-import state)))
           (POST "/upload" {params :multipart-params}
                 (r/response (import/import-capture! (assoc state :session session)
                                                      (edn/read-string (get params "session-camera-id"))
                                                      (get params "file"))))))
