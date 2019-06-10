(ns camelot.http.api.site.resources
  (:require
   [camelot.http.api.site.spec :as spec]
   [camelot.model.site :as site]
   [cats.core :as m]
   [cats.monad.either :as either]
   [clojure.edn :as edn]
   [ring.util.http-response :as hr]
   [camelot.http.api.util :as api-util]))

(def resource-type :site)

(def resource-base-uri "/api/v1/sites")

(defn patch! [state id data]
  (let [mr (m/->>=
           (either/right data)
           (api-util/transform-request resource-type :camelot.http.api.site.patch/attributes id)
           (site/patch! state (edn/read-string id))
           (api-util/transform-response resource-type ::spec/attributes))]
    (->> mr
         (m/fmap hr/ok)
         (api-util/to-response))))

(defn post! [state data]
  (let [mr (m/->>=
            (either/right data)
            (api-util/transform-request resource-type :camelot.http.api.site.post/attributes)
            (site/post! state)
            (api-util/transform-response resource-type ::spec/attributes))]
    (->> mr
         (m/fmap #(api-util/created resource-base-uri %))
         (api-util/to-response))))

(defn get-with-id [state id]
  (let [mr (m/->>= (either/right id)
                   (api-util/transform-id)
                   (site/get-single state)
                   (api-util/transform-response resource-type ::spec/attributes))]
    (->> mr
         (m/fmap hr/ok)
         (api-util/to-response))))

(defn get-all [state]
  (let [mr (m/->>= (either/right (site/get-all state))
                   (api-util/transform-response resource-type ::spec/attributes))]
    (->> mr
         (m/fmap hr/ok)
         (api-util/to-response))))

(defn delete! [state id]
  (let [mr (site/mdelete! state (edn/read-string id))]
    (->> mr
         (m/fmap (constantly (hr/no-content)))
         (api-util/to-response))))
