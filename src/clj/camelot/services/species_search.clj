(ns camelot.services.species-search
  (:require
   [clj-http.client :as http]
   [clojure.core.async :refer [go <!]]
   [cheshire.core :as json]
   [camelot.model.taxonomy :as taxonomy]
   [camelot.model.associated-taxonomy :as ataxonomy]))

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

(defn result->tassociated-taxonomy
  [state survey-id result]
  (let [clsn (or (get-in result ["classification"])
                 (get-in result ["accepted_name" "classification"]))
        names (or (get-in result ["common_names"])
                  (get-in result ["accepted_name" "common_names"]))]
    (ataxonomy/tassociated-taxonomy
     {:taxonomy-class (classification-for clsn "Class")
      :taxonomy-order (classification-for clsn "Order")
      :taxonomy-family (classification-for clsn "Family")
      :taxonomy-genus (get result "genus")
      :taxonomy-species (get result "species")
      :citation (get result "bibliographic_citation")
      :taxonomy-common-name (common-name state names)
      :survey-id survey-id})))

(defn get-taxonomy-for-id
  [state id]
  (let [resp (query-lookup state {:id id})
        rs (get resp "results")]
    (if (or (zero? (get resp "number_of_results_returned"))
            (zero? (count rs)))
      (throw (RuntimeException. (str "Species ID lookup failed: " id)))
      (first rs))))

(defn create-taxonomy-with-id
  [state survey-id id]
  (->> id
       (get-taxonomy-for-id state)
       (result->tassociated-taxonomy state survey-id)
       (ataxonomy/create! state)))

(defn get-or-create-species
  [state survey-id details]
  (let [ttax (taxonomy/ttaxonomy {:taxonomy-species (:species details)
                                  :taxonomy-genus (:genus details)})]
    (let [ts (taxonomy/get-specific-by-taxonomy state ttax)]
      (if ts
        (ataxonomy/ensure-associated state survey-id (:taxonomy-id ts))
        (create-taxonomy-with-id state survey-id (:id details))))))

(defn ensure-survey-species-known
  [state species survey-id]
  (map (partial get-or-create-species state survey-id) species))
