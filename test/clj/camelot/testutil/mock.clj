(ns camelot.testutil.mock
  "Function stubs and mocks."
  (:require
   [clojure.string :as string]))

(def ^:dynamic *invocations* nil)

(defmacro with-spies [[args] & body]
  "Record invocations arguments for mocks created with defmock."
  `(let [ts# (transient {})
         ~args ts#]
     (binding [*invocations* ts#]
       ~@body)))

(defmacro defmock [ps & body]
  "Create a mock with the given params that returns `body.'"
  (let [fname# (gensym "mock-")]
    `(fn fname# ~ps
       (when ~`*invocations*
         (assoc! ~`*invocations* fname#
                 (conj (get ~`*invocations* fname#) ~ps)))
       ~@body)))

(defn- clj-key
  [acc k v]
  (assoc acc (keyword (string/replace (name k) #"_" "-")) v))

(defn query-params [args state ks]
  "Return arguments for the query function at `ks'."
  (->> ks
       (concat [:database :queries])
       vec
       (get-in state)
       (get args)
       (map first)
       (map #(reduce-kv clj-key {} %))
       (map #(dissoc % :current-timestamp))))
