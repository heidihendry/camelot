(ns camelot.http.api.dataset.resources
  (:require
   [slingshot.slingshot :as ss]
   [camelot.http.api.dataset.spec :as spec]
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

(defn backup! [state id]
  (ss/try+
   (let [definitions (datasets/get-definitions (:datasets state))
         dataset-id (keyword id)]
     (if-let [dataset (get definitions dataset-id)]
       (do
         (.backup (:backup-manager state) dataset)
         (hr/no-content))
       (hr/not-found)))
   (catch Object e
     (api-util/handle-error-response e))))

(defn get-datasets [state]
  (ss/try+
   (let [ds (:datasets state)
         connected (datasets/get-available ds)
         definitions (datasets/get-definitions ds)]
     (hr/ok (api-util/transform-response resource-type ::spec/attributes
                                         (map (fn [[k v]]
                                                (assoc v
                                                       :dataset-id (name k)
                                                       :isConnected (contains? connected k)))
                                              definitions))))
   (catch Object e
     (api-util/handle-error-response e))))
