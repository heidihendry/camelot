(ns camelot.util.rest
  (:require [camelot.util.config :as conf]
            [clojure.edn :as edn]
            [camelot.util.cursorise :refer [cursorise decursorise]]
            [camelot.application :as app]
            [clojure.tools.logging :as log]
            [ring.util.response :as r]
            [clj-time.coerce :as c]))

(def floating-point-fields
  "Set of keys for floating-point fields."
  #{:trap-station-longitude :trap-station-latitude :trap-station-altitude
    :trap-station-distance-above-ground :trap-station-distance-to-road
    :trap-station-distance-to-river :trap-station-distance-to-settlement})

(defn- as-long
  "Return value as a Long if it can be parsed, otherwise return v."
  [v]
  (if (and (instance? String v)
           (re-find #"[0-9]" v))
    (try (edn/read-string v)
         (catch java.lang.Exception e
           (do
             (log/warn "as-long: Attemp to read-string on " v)
             v)))
    v))

(defn- as-date
  [v]
  (c/from-long ^java.lang.Long (Long/parseLong v)))

(defn- normalise-field-types
  "Reducer to parse strings for ID fields."
  [acc k v]
  (if (re-find #"-id$" (name k))
    (assoc acc k (as-long v))
    (assoc acc k v)))

(defn parse-ids
  "Return data with any strings in ID fields parsed."
  [data]
  (reduce-kv normalise-field-types {} data))

(defn coerce-string-fields
  "Reducer to parse strings for various field types."
  [data]
  (reduce-kv
   (fn [acc k v]
     (cond
       (re-find #"-id$" (name k))
       (assoc acc k (as-long v))

       (re-find #"-date$" (name k))
       (assoc acc k (as-date v))

       (re-find #"-num$" (name k))
       (assoc acc k (edn/read-string v))

       (re-find #"-long$" (name k))
       (assoc acc k (Long/parseLong v))

       (re-find #"-float$" (name k))
       (assoc acc k (Float/parseFloat v))

       :else (assoc acc k v)))
   {} data))

(defn- parse-float-reducer
  "Reducer to parse strings for floating-point fields."
  [acc k v]
  (if (and (some #{k} floating-point-fields)
           (instance? String v))
    (assoc acc k (edn/read-string v))
    (assoc acc k v)))

(defn- parse-floats
  "Return `data' with floating point fields parsed from strings."
  [data]
  (reduce-kv parse-float-reducer {} data))

(defn- build-uri
  "Return the URI for the given resource."
  [resource-key id]
  (format "/%s/%s"
          (if (= (name resource-key) "taxonomy")
            "taxonomy"
            (if (= (name resource-key) "media")
              "media"
              (str (name resource-key) "s")))
          id))

(defn- resource-key-to-id
  "Return a field ID keyword for a given resource keyword."
  [resource-key]
  (-> resource-key
      (name)
      (str "-id")
      (keyword)))

(defn- resource-uri-generator
  "Returns a reducing function which assocs a uri for the given resource key."
  [resource-key]
  (fn [acc x]
    (conj acc (assoc x :uri (build-uri resource-key
                                       (get x (resource-key-to-id resource-key)))))))

(defn- add-resource-uris
  "Add URIs to all resources in `data'."
  [data resource-key]
  (reduce (resource-uri-generator resource-key) [] data))

(defn list-available
  "Return a list of the available resources for `id-str'.
  `f' is a function which takes the configuration as its first argument, and
  an as its second argument.  `id-str' the string representation of the
  desired resource ID.  This is typically used for finding possible resources
  where there's a constraint that they must be unique."
  [f id-str]
  (let [id (as-long id-str)]
    (-> (conf/config)
        (app/gen-state)
        (f id)
        (r/response))))

(defn list-resources
  "Return a list of resources.
  `f' is a function which takes the configuration as its first argument.
  `resource-key' is a key which will be used to generate a resource URI for
  each record.  It should be the key representing the ID of a record, sans the
  '-id' suffix.  If `id-str' is provided, this will be parsed and passed as
  the second argument to `f'."
  ([f resource-key]
   (-> (conf/config)
       (app/gen-state)
       (f)
       (add-resource-uris resource-key)
       (r/response)))
  ([f resource-key id-str]
   (let [id (as-long id-str)]
     (-> (conf/config)
         (app/gen-state)
         (f id)
         (add-resource-uris resource-key)
         (r/response)))))

(defn specific-resource
  "Return a single resource.
  `f' is a resource-fetching function which takes the configuration as its
  first argument, and a parsed ID is its second argument."
  [f id-str]
  (let [id (as-long id-str)]
    (-> (conf/config)
        (app/gen-state)
        (f id)
        (cursorise)
        (r/response))))

(defn update-resource
  "Return a single resource.
  `f' is a resource-fetching function which takes
  - the configuration as its first argument,
  - a parsed ID is its second argument,
  - a (processed) map as its third argument"
  ([f id-str data]
   (let [stddata (-> data (decursorise) (parse-ids) (parse-floats))
         id (as-long id-str)]
     (-> (conf/config)
         (app/gen-state)
         (f id stddata)
         (cursorise)
         (r/response))))
  ([f id-str ctor data]
   (let [stddata (-> data (decursorise) (parse-ids) (parse-floats) (ctor))
         id (as-long id-str)]
     (-> (conf/config)
         (app/gen-state)
         (f id stddata)
         (cursorise)
         (r/response)))))

(defn create-resource
  "Create a single resource.
  `f' is a function which creates a resource.  It will take the configuration
  as its first argument, and a (processed) map of the data for the resource as
  its second argument."
  ([f data]
   (let [stddata (-> data (decursorise) (parse-ids) (parse-floats))]
     (-> (conf/config)
         (app/gen-state)
         (f stddata)
         (cursorise)
         (r/response))))
  ([f ctor data]
   (let [stddata (-> data (decursorise) (parse-ids) (parse-floats) (ctor))]
     (-> (conf/config)
         (app/gen-state)
         (f stddata)
         (cursorise)
         (r/response)))))

(defn delete-resource
  "Delete a resource.
  `f' is a function which deletes a resource given a (parsed) ID.  `id-str' is
  the string representation of a resource to delete."
  [f id-str]
  (let [id (as-long id-str)]
    (-> (conf/config)
        (app/gen-state)
        (f id)
        ((fn [v] (hash-map :data v)))
        (r/response))))
