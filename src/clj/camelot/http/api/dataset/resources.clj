(ns camelot.http.api.dataset.resources
  (:require
   [slingshot.slingshot :as ss]
   [camelot.state.datasets :as datasets]
   [ring.util.http-response :as hr]
   [camelot.http.api.util :as api-util]))

(def resource-type :dataset)
(def resource-base-uri "/api/v1/dataset")

(defn connect! [state id]
  (ss/try+
    (datasets/connect! (:datasets state) (keyword id))
    (hr/no-content)
    (catch Object e
      (api-util/handle-error-response e))))

(defn disconnect! [state id]
  (ss/try+
   (datasets/disconnect! (:datasets state) (keyword id))
   (hr/no-content)
   (catch Object e
     (api-util/handle-error-response e))))

(defn reload! [state]
  (ss/try+
   (datasets/reload! (:datasets state))
   (hr/no-content)
   (catch Object e
     (api-util/handle-error-response e))))
