(ns camelot.http.report
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET]]
   [camelot.report.core :as report]
   [camelot.util.crud :as crud]))

(def routes
  (context "/report" {state :state}
           (GET "/manage/rescan" [] (do (report/refresh-reports state)
                                        {:status 200
                                         :headers {"Content-Type" "text/plain; charset=utf-8"}
                                         :body "Reports rescanned."}))
           (GET "/:report/download" {params :params}
                (report/export
                 state
                 (keyword (:report params)) (crud/coerce-string-fields params)))
           (GET "/:report" [report] (r/response (report/get-configuration
                                                 state
                                                 (keyword report))))
           (GET "/" [] (r/response (report/available-reports state)))))
