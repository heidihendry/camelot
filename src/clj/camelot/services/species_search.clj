(ns camelot.services.species-search
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [go <!]]
            [cheshire.core :as json]))

(def service-url "http://www.catalogueoflife.org/col/webservice")

(defn query-lookup
  [state {:keys [id]}]
  (let [r (http/get service-url
                    {:accept "json"
                     :query-params {"id" id
                                    "format" "json"}})]
    (json/parse-string (:body r))))

(defn query-search
  [state {:keys [search]}]
  (let [r (http/get service-url
                    {:accept "json"
                     :query-params {"name" (str search "*")
                                    "format" "json"}})]
    (json/parse-string (:body r))))

(defn query-search-full
  [state {:keys [search]}]
  (let [r (http/get service-url
                    {:accept "json"
                     :query-params {"name" (str search "*")
                                    "format" "json"
                                    "response" "full"}})]
    (json/parse-string (:body r))))

(defn create-for-ids
  [state ids]
  nil)
