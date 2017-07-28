(ns camelot.testutil.mock
  "Function stubs and mocks."
  (:require
   [clojure.string :as string]
   [clojure.test :refer :all]))

(def ^:dynamic *invocations* nil)

(defmacro with-spies [[args] & body]
  "Record invocations arguments for mocks created with defmock."
  `(let [ts# (atom {})
         ~args #(::fns @ts#)]
     (binding [*invocations* ts#]
       ~@body)))

(defmacro defmock [ps & body]
  "Create a mock with the given params that evaluates `body.'"
  (let [fname# (gensym "mock-")]
    `(fn fname# ~ps
       (reset! ~`*invocations*
               (some-> @~`*invocations*
                       (update-in [::fns fname#] #(conj (vec %) ~ps))
                       (update ::order #(conj (vec %) fname#))))
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

(defn query-order-is
  [args state & ks]
  (prn *invocations*)
  (let [fns (->> ks
                 (map #(concat [:database :queries] %))
                 (map vec)
                 (map #(get-in state %)))]
    (is (= (count ks) (count fns)))
    (is (= (filter (set fns) (::order @*invocations*)) fns))))
