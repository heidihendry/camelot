(ns camelot.library.query.translate
  (:require [camelot.library.query.fields :as fields]
            [camelot.util.datatype :as datatype-util]
            [bitpattern.simql.parser.datatypes :as simql-dt]
            [clj-time.coerce :as tc]))

(defmethod simql-dt/read-value :date
  [k vs]
  (let [v (first (filter string? vs))]
    (if (datatype-util/could-be-date? v)
      [k (datatype-util/as-date v)]
      (throw (ex-info (str "A date was required but was not found")
                      {:field-value vs})))))

(defmethod simql-dt/read-value :timestamp
  [_ vs]
  (simql-dt/read-value :date vs))

(defmethod simql-dt/read-value :readable-integer
  [_ vs]
  (simql-dt/read-value :integer vs))

(def ^:dynamic *complement* false)

(defmacro ... [k rs]
  `(apply vector ~k ~rs))

(defmacro with-complement
  [& body]
  `(binding [*complement* (not *complement*)]
     ~@body))

(defn and-operator []
  (if *complement* :or :and))

(defn or-operator []
  (with-complement (and-operator)))

(defmulti operator identity)

(defn eq-operator []
  (if *complement* :<> :=))

(defn gt-operator []
  (if *complement* :<= :>))

(defn lt-operator []
  (if *complement* :>= :<))

(defn neq-operator []
  (with-complement (eq-operator)))

(defn geq-operator []
  (with-complement (lt-operator)))

(defn leq-operator []
  (with-complement (gt-operator)))

(defmulti operator->sql identity)
(defmethod operator->sql ">" [_] (gt-operator))
(defmethod operator->sql "<" [_] (lt-operator))
(defmethod operator->sql "<=" [_] (leq-operator))
(defmethod operator->sql ">=" [_] (geq-operator))
(defmethod operator->sql "!=" [_] (neq-operator))
(defmethod operator->sql ":" [_] (eq-operator))
(defmethod operator->sql "=" [_] (eq-operator))
(defmethod operator->sql "==" [_] (eq-operator))
(defmethod operator->sql :default [o]
  (throw (ex-info "Unsupported operator" {:op o})))

(defn like-operator []
  (if *complement* :not-like :like))

(defmulti datatype->sql first)

(defn- value->fragment
  [v]
  (if (vector? v)
    (datatype->sql v)
    v))

(defmethod datatype->sql :wildcard [_] "%")
(defmethod datatype->sql :dblquote [_] "")

(defmethod datatype->sql :string
  [[_ & vs]]
  [:lower (apply str (mapv value->fragment vs))])

(defmethod datatype->sql :timestamp
  [[_ v]]
  (tc/to-long v))

(defmethod datatype->sql :date
  [[_ v]]
  (tc/to-long v))

(defmethod datatype->sql :default
  [[_ & vs]]
  (first vs))

(defmulti parse->sql first)

(defmethod parse->sql :field
  [[_ f]]
  (fields/field->column f))

(defmethod parse->sql :operator
  [[_ o]]
  (operator->sql o))

(defn- like?
  [vs]
  (some #{[:wildcard]} vs))

(defn- like
  [fc v]
  [(like-operator) fc v])

(defn- existence?
  [vs]
  (or (= vs [[:wildcard]])
      (= vs [[:dblquote] [:wildcard] [:dblquote]])))

(defn- existence
  [fc dt]
  (let [rel (and-operator)
        op (neq-operator)]
    (cond
      (= dt :integer) [rel [op fc nil] [op fc 0]]
      (= dt :boolean) [op fc nil]
      :else [rel [op fc nil] [op fc ""]])))

(defmethod parse->sql :field-value
  [[_ fv]]
  (datatype->sql fv))

(defn- field-column
  [dt f]
  (let [fc (parse->sql f)]
    (if (= dt :string)
      [:lower fc]
      fc)))

(defmethod parse->sql :field-search
  [[_ f op [_ [dt & vs] :as v]]]
  (let [fc (field-column dt f)]
    (cond
      (existence? vs) (existence fc dt)
      (like? vs) (like fc (parse->sql v))
      :else [(parse->sql op)
             (fields/auto-coalesce-field dt fc)
             (parse->sql v)])))

(defmethod parse->sql :string-search
  [[_ sv]]
  (let [s (datatype->sql sv)]
    (... (or-operator)
         (mapv #(like [:lower %] s)
               (fields/full-text-fields)))))

(defmethod parse->sql :statement
  [[_ r]]
  (parse->sql r))

(defmethod parse->sql :conjunction
  [[_ & r]]
  (... (and-operator) (map parse->sql r)))

(defmethod parse->sql :disjunction
  [[_ & r]]
  (... (or-operator) (map parse->sql r)))

(defmethod parse->sql :expr-complement
  [[_ r]]
  (with-complement (parse->sql r)))

(defmethod parse->sql :default
  [[_ & r]]
  (vec (mapcat parse->sql r)))
