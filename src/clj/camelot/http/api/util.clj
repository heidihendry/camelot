(ns camelot.http.api.util
  (:require
   [cats.core :as m]
   [clojure.edn :as edn]
   [cats.monad.either :as either]
   [camelot.util.model :as model]
   [clj-time.coerce :as tc]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]
   [ring.util.http-response :as hr]
   [clojure.string :as cstr]))

(defprotocol ISerializable
  (serialize [this]))

(extend-protocol ISerializable
  org.joda.time.DateTime
  (serialize [this]
    (tc/to-long this)))

(extend-protocol ISerializable
  org.joda.time.DateTime
  (serialize [this]
    (tc/to-long this)))

(extend-protocol ISerializable
  clojure.lang.Keyword
  (serialize [this]
    (name this)))

(defn- canonicalize-key [type k]
  (-> k
      name
      (clojure.string/replace (re-pattern (str "^" (name type) "-")) "")
      (clojure.string/replace #"-(\w)"
                              (comp cstr/upper-case second))
      keyword))

(defn- canonicalize
  [type d]
  (cond
    (map? d)
    (reduce-kv (fn [acc k v] (assoc acc (canonicalize-key type k)
                                    (canonicalize type v))) {} d)

    (coll? d)
    (map (partial canonicalize type)  d)

    :default
    (if (extends? ISerializable (class d))
      (serialize d)
      d)))

(defn- restrict-keys
  [spec v]
  (st/select-spec spec v))

(defn- to-data
  [type spec x]
  (let [idk (keyword (str (name type) "-id"))]
    {:id (str (get x idk))
     :type (name type)
     :attributes (->> x
                      (canonicalize type)
                      (restrict-keys spec))}))

(defn transform-response
  [type spec x]
  (if (and (not (map? x)) (coll? x))
    (either/right {:data (map (partial to-data type spec) x)})
    (either/right {:data (to-data type spec x)})))

(defn- ednize-key [type k]
  (as-> k $
    (name $)
    (cstr/replace $ #"[A-Z]"
                  (comp #(str "-" %) cstr/lower-case))
    (str (name type) "-" $)
    (keyword $)))

(defn- timestamp-field?
  [k]
  (let [dt (get-in model/schema-definitions [k :datatype])]
    (#{:timestamp :date} dt)))

(defn- ednize [type d]
  (cond
    (map? d)
    (reduce-kv (fn [acc k v]
                 (let [nk (ednize-key type k)]
                   (assoc acc nk
                          (if (timestamp-field? nk)
                            (tc/from-long v)
                            (ednize type v)))))
               {} d)

    (coll? d)
    (map (partial ednize type) d)

    :default d))

(defn- from-request
  [type spec id m]
  (assoc
   (ednize type (restrict-keys spec (get-in m [:data :attributes])))
   (keyword (str (name type) "-id")) id))

(defn- post-request
  [type spec m]
  {:pre [(= type (keyword (get-in m [:data :type])))]}
  (ednize type (restrict-keys spec (get-in m [:data :attributes]))))

(defn- keywordize
  [d]
  (cond
    (map? d)
    (reduce-kv (fn [acc k v]
                 (assoc acc (keyword k) (keywordize v)))
               {} d)

    (coll? d)
    (map keywordize d)

    ;; TODO some things (e.g. dates, keywords) need to be deserialised
    :default d))

(defn transform-id
  [id]
  (if (re-matches #"[0-9]+" id)
    (either/right (edn/read-string id))
    (either/left {:error/type :error.type/bad-request})))

(defn transform-request
  ([resource-type spec data]
   (if (= resource-type (keyword (get-in data [:data :type])))
     (either/right (post-request resource-type spec data))
     (either/left {:error/type :error.type/bad-request})))
  ([resource-type spec id data]
   (if (and (= resource-type (keyword (get-in data [:data :type])))
            (= (str id) (get-in data [:data :id]))
            (re-matches #"[0-9]+" id))
     (either/right (from-request resource-type spec id data))
     (either/left {:error/type :error.type/bad-request}))))

(defn- handle-error-response
  [e]
  (condp = (:error/type e)
    :error.type/bad-request
    (hr/bad-request)

    :error.type/not-found
    (hr/not-found)

    :error.type/conflict
    (hr/conflict)

    (hr/internal-server-error)))

(defn created [base-uri response]
  (let [location (str base-uri "/" (get-in response [:data :id]))]
    (hr/created location response)))

(defn to-response [me]
  (m/extract (m/bimap handle-error-response identity me)))
