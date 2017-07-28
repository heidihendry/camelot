(ns camelot.testutil.mock
  "Function stubs and mocks."
  (:require
   [clojure.string :as string]
   [clojure.test :refer :all]))

(def ^:dynamic *invocations* nil)

(defmacro with-spies [[calls] & body]
  "Record invocations arguments for mocks created with defmock."
  `(let [ts# (atom {})
         ~calls (fn [] (::fns @ts#))]
     (binding [*invocations* ts#]
       ~@body)))

(defmacro defmock [ps & body]
  "Create a mock with the given params that evaluates `body.'"
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

(defn query-params [calls state ks]
  "Return arguments for the query function at `ks'."
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
