(ns camelot.util.datatype
  "Datatypes, constraints and deseralisation for bulk import."
  (:require
   [camelot.util.model :as model]
   [clojure.java.io :as io]
   [camelot.util.sighting-fields :as util.sf]
   [clj-time.format :as tf]
   [clojure.edn :as edn]
   [camelot.util.trap-station :as trap]
   [camelot.util.file :as file]
   [clojure.tools.logging :as log]))

(defn read-metadata-string
  "Read str as edn, or :error if not readable."
  [str]
  (when str
    (try
      (edn/read-string str)
      (catch java.lang.Exception e
        :error))))

(def timestamp-formatters
  "Additional date-time formatters."
  [(tf/formatter "yyyy-M-d H:m:s")
   (tf/formatter "yyyy/M/d H:m:s")
   (tf/formatter "E MMM d H:m:s Z yyyy")
   (tf/formatter "yyyy:M:d H:m:s")])

(def date-formatters
  "Additional date formatters."
  (concat timestamp-formatters
          [(tf/formatter "yyyy:M:d")
           (tf/formatter "yyyy-M-d")
           (tf/formatter "yyyy/M/d")]))

(defn parse-date-time
  "Parse a date-time, returning nil if not parsable."
  [fmt x]
  (try
    (tf/parse fmt x)
    (catch Exception _ nil)))

(defn as-datetime
  "Return a date-time attempting deseralisation using `timestamp-formatters`
  and built-in formatters."
  [x]
  (when (seq x)
    (or (some #(parse-date-time % x) timestamp-formatters)
        (tf/parse x))))

(defn as-date
  "Return a date-time attempting deseralisation using `date-formatters` and
  built-in formatters."
  [x]
  (when (seq x)
    (or (some #(parse-date-time % x) (concat timestamp-formatters date-formatters))
        (tf/parse x))))

(defn could-be-timestamp?
  "Predicate returning true if input can be parsed as a date-time, false
  otherwise."
  [x]
  (cond
    (empty? x) true
    (< (count x) 14) false
    (some? (some #(parse-date-time % x) timestamp-formatters)) true
    (tf/parse x) true
    :else false))

(defn could-be-date?
  "Predicate returning true if input can be parsed as a date, false
  otherwise."
  [x]
  (if (or (could-be-timestamp? x)
          (some? (some #(parse-date-time % x) date-formatters)))
    true
    false))

(defn could-be-number?
  "Predicate returning true if input is a an integer or floating point number,
  false otherwise."
  [x]
  (if (or (empty? x)
          (seq (re-matches #"^-?[0-9]+(\.[0-9]+)?$" x)))
    true
    false))

(defn could-be-integer?
  "Predicate returning true if input is an integer, false otherwise."
  [x]
  (if (or (empty? x)
          (seq (re-matches #"^-?[0-9]+$" x)))
    true
    false))

(defn could-be-readable-integer?
  "Predicate returning true if input can be parsed as an integer, false otherwise.

  Unlike `could-be-integer?', this permits non-numeric input such as units
  following the integer."
  [x]
  (let [r (read-metadata-string x)]
    (if (or (nil? r)
            (integer? r))
      true
      false)))

(defn as-boolean
  "Parse input to a boolean, given a number of variants of true.

  Input which does not confirm to a `true' value is assumed to be false."
  [x]
  (if (empty? x)
    false
    (if (seq (re-matches #"^(?i)(1|t(rue)?|y(es)?)$" x))
      true
      false)))

(defn could-be-yes-no?
  "Predicate returning true if input is a Yes/No-like value."
  [x]
  (or (empty? x)
      (seq (re-matches #"^(?i)y(es)?|n(o)?$" x))))

(defn could-be-zero-one?
  "Predicate returning true if input is a 1/0-like value."
  [x]
  (or (empty? x)
      (seq (re-matches #"0|1" x))))

(defn could-be-true-false?
  "Predicate returning true if input is a True/False-like value."
  [x]
  (or (empty? x)
      (seq (re-matches #"^(?i)T(rue)?|F(alse)?$" x))))

(defn could-be-boolean?
  "Predicate returning true if input is any of yes/no, 1/0 or true/false-like."
  [x]
  (if (or (could-be-yes-no? x)
          (could-be-zero-one? x)
          (could-be-true-false? x))
    true
    false))

(defn could-be-longitude?
  "Predicate returning true if input could be a longitude in decimal format."
  [x]
  (if (empty? x)
    true
    (if (try
          (trap/valid-longitude? (Double/parseDouble x))
          (catch Exception _ nil))
      true
      false)))

(defn could-be-latitude?
  "Predicate returning true if input could be a latitude in decimal format."
  [x]
  (if (empty? x)
    true
    (if (try
          (trap/valid-latitude? (Double/parseDouble x))
          (catch Exception _ nil))
      true
      false)))

(defn as-sex
  "Return a single-character indicator of the sex."
  [v]
  (when (seq v)
    (if (seq (re-matches #"^(?i)M(ale)?$" v))
      "M"
      "F")))

(defn could-be-sex?
  "Predicate returning true if the input could be a sex, false otherwise."
  [x]
  (if (or (empty? x)
          (seq (re-matches #"^(?i)M(ale)?|F(emale)?$" x)))
    true
    false))

(defn as-lifestage
  "Return the lifestage. Defaults to Juvenile if non-adult."
  [v]
  (when (seq v)
    (if (seq (re-matches #"^(?i)A(dult)?$" v))
      "adult"
      "juvenile")))

(defn could-be-lifestage?
  "Predicate returning true if the input could be a lifestage, false otherwise."
  [x]
  (if (or (empty? x)
          (seq (re-matches #"^(?i)A(dult)?|J(uvenile)?$" x)))
    true
    false))

(defn could-be-file?
  "Predicate returning true if input is a readable file, false otherwise."
  [x]
  (let [f (file/->file x)]
    (and f
         (file/exists? f)
         (file/readable? f)
         (file/file? f))))

(defn is-user-field?
  "Predicate returning true if a key is for a sighting field."
  [x]
  (re-find (re-pattern (str "^" util.sf/user-key-prefix)) (name x)))

(defn could-be-required?
  "Predicate returning true if input is non-blank and non-nil."
  [x]
  (if (seq x) true false))

(def constraint-check-fns
  "Mapping between constraint token and its predicate"
  {:required could-be-required?})

(def datatype-check-fns
  "Mapping between the datatype token and its predicate."
  {:timestamp could-be-timestamp?
   :date could-be-date?
   :number could-be-number?
   :integer could-be-integer?
   :readable-integer could-be-readable-integer?
   :boolean could-be-boolean?
   :latitude could-be-latitude?
   :longitude could-be-longitude?
   :sex could-be-sex?
   :lifestage could-be-lifestage?
   :file could-be-file?
   :string (constantly true)})

(defn matches-all
  "Given a mapping of tokens to predicates, return the set of tokens which all
  items in a list are true."
  [check-fns xs]
  (disj (->> check-fns
             (map (fn [[r cf]] (when (every? cf xs) r)))
             set)
        nil))

(defn possible-constraints
  "Return set of constraints which every member of the input satisfies."
  [xs]
  (matches-all constraint-check-fns xs))

(defn possible-datatypes
  "Return set of datatypes which every member of the input satisfies."
  [xs]
  (matches-all datatype-check-fns xs))

(defn max-length
  "Return maximum string length of the column."
  [xs]
  (reduce #(max %1 (count %2)) 0 xs))

(def deserialisers
  "Mapping between effective datatypes and its deserialisation function."
  {:integer edn/read-string
   :readable-integer edn/read-string
   :number edn/read-string
   :sex as-sex
   :lifestage as-lifestage
   :timestamp as-datetime
   :date as-date
   :longitude edn/read-string
   :latitude edn/read-string
   :boolean as-boolean
   :file io/file
   :string identity})

(defn deserialise
  "Deserialise value from a string to the given datatype."
  [datatype value]
  (when (some? (some #{datatype} (possible-datatypes [value])))
    (let [f (get deserialisers datatype)]
      (f value))))

(defn deserialise-field
  "Deserialise a string given its field and (optionally) given a map of schemas."
  ([schemas field str-value]
   (if (is-user-field? field)
     str-value
     (if-let [s (get schemas field)]
       (if-let [f (get deserialisers (model/effective-datatype s))]
         (f str-value)
         str-value))))
  ([field str-value]
   (deserialise-field model/all-mappable-fields field str-value)))
