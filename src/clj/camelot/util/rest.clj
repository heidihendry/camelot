(ns camelot.util.rest
  (:require [ring.util.response :as r]
            [camelot.processing.settings :refer [gen-state config cursorise decursorise]]))

(defn- normalise-field-types
  [acc k v]
  (if (and (re-find #"-id$" (name k))
           (instance? String v))
    (assoc acc k (read-string v))
    (assoc acc k v)))

(def floating-point-fields
  #{:trap-station-longitude :trap-station-latitude})

(defn parse-float-reducer
  [acc k v]
  (if (some #{k} floating-point-fields)
    (assoc acc k (read-string v))
    (assoc acc k v)))

(defn parse-floats
  [data]
  (reduce-kv parse-float-reducer {} data))

(defn parse-ids
  [data]
  (reduce-kv normalise-field-types {} data))

(defn- build-uri
  [resource-key id]
  (format "/%ss/%s"
          (name resource-key)
          id))

(defn- resource-key-to-id
  [resource-key]
  (-> resource-key
      (name)
      (str "-id")
      (keyword)))

(defn- resource-uri-generator
  [resource-key]
  (fn [acc x]
    (conj acc (assoc x :uri (build-uri resource-key
                                       (get x (resource-key-to-id resource-key)))))))

(defn add-resource-uris
  [data resource-key]
  (reduce (resource-uri-generator resource-key) '() data))

(defn list-available
  [f id-str]
  (let [id (read-string id-str)]
    (-> (config)
        (gen-state)
        (f id)
        (r/response))))

(defn list-resources
  ([f resource-key]
   (-> (config)
       (gen-state)
       (f)
       (add-resource-uris resource-key)
       (r/response)))
  ([f resource-key id-str]
   (let [id (read-string id-str)]
     (-> (config)
         (gen-state)
         (f id)
         (add-resource-uris resource-key)
         (r/response)))))

(defn specific-resource
  [f id-str]
  (let [id (read-string id-str)]
    (-> (config)
        (gen-state)
        (f id)
        (cursorise)
        (r/response))))

(defn update-resource
  [f id-str data]
  (let [stddata (parse-floats (parse-ids (decursorise data)))
        id (read-string id-str)]
    (-> (config)
        (gen-state)
        (f id stddata)
        (cursorise)
        (r/response))))

(defn create-resource
  [f data]
  (let [stddata (parse-ids (decursorise data))]
    (-> (config)
        (gen-state)
        (f stddata)
        (cursorise)
        (r/response))))

(defn delete-resource
  [f id-str]
  (let [id (read-string id-str)]
    (-> (config)
        (gen-state)
        (f id)
        ((fn [v] (hash-map :data v)))
        (r/response))))
