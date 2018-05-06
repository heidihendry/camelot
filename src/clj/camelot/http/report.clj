(ns camelot.http.report
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET]]
   [camelot.report.core :as report]
   [camelot.util.crud :as crud]))

(def routes
  (context "/report" {session :session state :system}
           (GET "/manage/rescan" [] (do (report/refresh-reports state)
                                        {:status 200
                                         :headers {"Content-Type" "text/plain; charset=utf-8"}
                                         :body "Reports rescanned."}))
           (GET "/:report/download" {params :params}
                (report/export
                 (assoc state :session session)
                 (keyword (:report params)) (crud/coerce-string-fields params)))
           (GET "/:report" [report] (r/response (report/get-configuration
                                                 (assoc state :session session)
                                                 (keyword report))))
           (GET "/" [] (r/response (report/available-reports (assoc state :session session))))))
