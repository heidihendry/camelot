(ns camelot.http.api.core
  (:require
   [compojure.api.sweet :refer [api context]]
   [camelot.http.api.survey.core :as survey]
   [camelot.http.api.site.core :as site]
   [clojure.tools.logging :as log]
   [ring.util.http-response :as hr]
   [camelot.http.api.dataset.core :as dataset]))

(def routes
  (context "/api/v1" []
    :coercion :spec
    dataset/routes
    survey/routes
    site/routes))

(defn- exception-handler
  [e _ _]
  (log/error "Exception while processing request:" e)
  (hr/internal-server-error {:type "unknown-exception" :class (.getName (.getClass e))}))

(def core-api
  (api
   {:exceptions {:handlers {:compojure.api.exception/default exception-handler}}
    :swagger
    {:ui "/api"
     :spec "/api/v1/swagger.json"
     :data {:info {:title "Camelot API"
                   :description "RESTful API for Camelot"}}}}
   routes))
