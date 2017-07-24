(ns camelot.test-util.mock-db
  (:require
   [camelot.util.db :as db]))

(def ^:private ^:dynamic *arguments* nil)
(def ^:dynamic *mock-ns* nil)

(defmacro with-args [[args] & body]
  `(let [ts# (transient {})
         ~args ts#]
     (binding [*arguments* ts#]
       ~@body)))

(defmacro with-mocking
  [& body]
  `(binding [~`*mock-ns* (create-ns (gensym "mockns-"))]
     ~@body))

(defmacro defmock [n var]
  `(intern ~`*mock-ns* '~n (fn [sym#] ~var)))

(defn with-mocked-db
  [f]
  (fn [& args]
    (with-redefs
      [db/fn-with-db-keys (fn [s dbf ps]
                            (let [fname (:name (meta dbf))]
                              (assoc! *arguments* fname
                                      (conj (or (get *arguments* fname) []) ps))
                              (eval (list (symbol (str *mock-ns*) (name fname))
                                          ps))))]
      (apply f args))))
