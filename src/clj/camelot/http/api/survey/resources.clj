(ns camelot.http.api.survey.resources
  (:require
   [camelot.http.api.survey.spec :as spec]
   [camelot.model.survey :as survey]
   [cats.core :as m]
   [cats.monad.either :as either]
   [clojure.edn :as edn]
   [ring.util.http-response :as hr]
   [camelot.http.api.util :as api-util]))

(def resource-type :survey)

(def resource-base-uri "/api/v1/surveys")

(defn patch [state id data]
  (let [mr (m/->>=
           (either/right data)
           (api-util/transform-request resource-type :camelot.http.api.survey.patch/attributes id)
           (survey/patch! state (edn/read-string id))
           (api-util/mtransform-response resource-type ::spec/attributes))]
    (->> mr
         (m/fmap hr/ok)
         (api-util/to-response))))

(defn post [state data]
  (let [mr (m/->>=
            (either/right data)
            (api-util/transform-request resource-type :camelot.http.api.survey.post/attributes)
            (survey/post! state)
            (api-util/mtransform-response resource-type ::spec/attributes))]
    (->> mr
         (m/fmap #(api-util/created resource-base-uri %))
         (api-util/to-response))))

(defn get-with-id [state id]
  (let [mr (m/->>= (either/right id)
                   (api-util/transform-id)
                   (survey/get-single state)
                   (api-util/mtransform-response resource-type ::spec/attributes))]
    (->> mr
         (m/fmap hr/ok)
         (api-util/to-response))))

(defn get-all [state]
  (let [mr (m/->>= (either/right (survey/get-all state))
                   (api-util/mtransform-response resource-type ::spec/attributes))]
    (->> mr
         (m/fmap hr/ok)
         (api-util/to-response))))

(defn delete [state id]
  (let [mr (survey/mdelete! state id)]
    (->> mr
         (m/fmap (constantly (hr/no-content)))
         (api-util/to-response))))
