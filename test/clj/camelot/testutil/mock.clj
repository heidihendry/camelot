(ns camelot.testutil.mock
  "Function stubs and mocks."
  (:require
   [camelot.system.protocols :as protocols]
   [camelot.state.datasets :as datasets]
   [clojure.string :as string]
   [clojure.test :refer :all]))

(def ^:dynamic *invocations* nil)

(defmacro with-spies
  "Record invocations arguments for mocks created with defmock."
  [[calls] & body]
  `(let [ts# (atom {})
         ~calls (fn [] (::fns @ts#))]
     (binding [*invocations* ts#]
       ~@body)))

(defmacro defmock
  "Create a mock with the given params that evaluates `body.'"
  [ps & body]
  (let [fname# (gensym "mock-")]
    `(fn fname# ~ps
       (swap! ~`*invocations*
              (fn [x#] (-> x#
                          (update-in [::fns fname#] #(conj (vec %) ~ps))
                          (update ::order #(conj (vec %) fname#)))))
       ~@body)))

(defn- clj-key
  [acc k v]
  (assoc acc (keyword (string/replace (name k) #"_" "-")) v))

(defn query-params
  "Return arguments for the query function at `ks'."
  [calls state ks]
  (->> ks
       (concat [:database :queries])
       vec
       (get-in state)
       (get calls)
       (map first)
       (map #(reduce-kv clj-key {} %))
       (map #(dissoc % :current-timestamp))))

(defn query-order-is
  [calls state & ks]
  (let [fns (->> ks
                 (map #(concat [:database :queries] %))
                 (map vec)
                 (map #(get-in state %)))]
    (is (= (count ks) (count fns)))
    (is (= (filter (set fns) (::order @*invocations*)) fns))))

(defrecord MockDatasets [datasets]
  protocols/Reloadable
  (reload [this]
    this)

  protocols/Inspectable
  (inspect [this]
    {:datasets/available (set (keys datasets))
     :datasets/definitions datasets})

  protocols/Contextual
  (set-context [this k v]
    (assoc-in this [::context k] v))

  (context [this k]
    (get-in this [::context k])))

(defn datasets
  ([ds]
   (map->MockDatasets {:datasets ds}))
  ([ds context]
   (let [record (map->MockDatasets {:datasets ds})]
     (if context
       (datasets/assoc-dataset-context record context)
       record))))
