(ns camelot.services.species-search
  (:require [clj-http.client :as http]
            [clojure.core.async :refer [go <!]]
            [cheshire.core :as json]
            [camelot.model.taxonomy :as taxonomy]))

(def service-url "http://www.catalogueoflife.org/col/webservice")

(defn query-lookup
  [state {:keys [id]}]
  (let [r (http/get service-url
                    {:accept "json"
                     :query-params {"id" id
                                    "format" "json"
                                    "response" "full"}})]
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

(defn common-name
  [state names]
  (if (zero? (count names))
    "N/A"
    (get (first names) "name")))

(defn rank=
  [rank rec]
  (= rank (get rec "rank")))

(defn classification-for
  [clsn rank]
  (->> clsn
       (filter (partial rank= rank))
       (first)
       (#(get % "name"))))

(defn result->ttaxonomy
  [state result]
  (let [clsn (or (get-in result ["classification"])
                 (get-in result ["accepted_name" "classification"]))
        names (or (get-in result ["common_names"])
                  (get-in result ["accepted_name" "common_names"]))]
    (taxonomy/ttaxonomy {:taxonomy-class (classification-for clsn "Class")
                         :taxonomy-order (classification-for clsn "Order")
                         :taxonomy-family (classification-for clsn "Family")
                         :taxonomy-genus (get result "genus")
                         :taxonomy-species (get result "species")
                         :taxonomy-common-name (common-name state names)})))

(defn get-taxonomy-for-id
  [state id]
  (let [resp (query-lookup state {:id id})
        rs (get resp "results")]
    (if (or (zero? (get resp "number_of_results_returned"))
            (zero? (count rs)))
      (throw (RuntimeException. (str "Species ID lookup failed: " id)))
      (first rs))))

(defn create-taxonomy-with-id
  [state id]
  (->> id
       (get-taxonomy-for-id state)
       (result->ttaxonomy state)
       (taxonomy/create! state)))

(defn get-or-create-species
  [state details]
  (let [ttax (taxonomy/ttaxonomy {:taxonomy-species (:species details)
                                  :taxonomy-genus (:genus details)})]
    (or (taxonomy/get-specific-by-taxonomy state ttax)
        (create-taxonomy-with-id state (:id details)))))

(defn ensure-survey-species-known
  [state species]
  (map (partial get-or-create-species state) species))
