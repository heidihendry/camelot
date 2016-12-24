(ns camelot.bulk-import.datatype
  (:require
   [camelot.util.model :as model]
   [clojure.java.io :as io]
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
  [(tf/formatter "yyyy-M-d H:m:s")
   (tf/formatter "yyyy/M/d H:m:s")
   (tf/formatter "E MMM d H:m:s Z yyyy")
   (tf/formatter "yyyy:M:d H:m:s")])

(def date-formatters
  [(tf/formatter "yyyy:M:d")
   (tf/formatter "yyyy-M-d")
   (tf/formatter "yyyy/M/d")])

(defn try-parse
  [fmt x]
  (try
    (tf/parse fmt x)
    (catch Exception _ nil)))

(defn as-datetime
  [x]
  (when (seq x)
    (or (some #(try-parse % x) timestamp-formatters)
        (tf/parse x))))

(defn as-date
  [x]
  (when (seq x)
    (or (some #(try-parse % x) (concat timestamp-formatters date-formatters))
        (tf/parse x))))

(defn could-be-timestamp?
  [x]
  (cond
    (empty? x) true
    (< (count x) 14) false
    (some? (some #(try-parse % x) timestamp-formatters)) true
    (tf/parse x) true
    :else false))

(defn could-be-date?
  [x]
  (if (or (could-be-timestamp? x)
          (some? (some #(try-parse % x) date-formatters)))
    true
    false))

(defn could-be-number?
  [x]
  (if (or (empty? x)
          (seq (re-matches #"^-?[0-9]+(\.[0-9]+)?$" x)))
    true
    false))

(defn could-be-integer?
  [x]
  (if (or (empty? x)
          (seq (re-matches #"^-?[0-9]+$" x)))
    true
    false))

(defn could-be-readable-integer?
  [x]
  (let [r (read-metadata-string x)]
    (if (or (nil? r)
            (integer? r))
      true
      false)))

(defn as-boolean
  [v]
  (if (empty? v)
    false
    (if (seq (re-matches #"^(1|(?i)t(rue)?|(?)y(es)?)$" v))
      true
      false)))

(defn- could-be-yes-no?
  [x]
  (or (empty? x)
      (seq (re-matches #"^(?i)y(es)?|n(o)?$" x))))

(defn- could-be-zero-one?
  [x]
  (or (empty? x)
      (seq (re-matches #"0|1" x))))

(defn- could-be-true-false?
  [x]
  (or (empty? x)
      (seq (re-matches #"^(?i)T(rue)?|F(alse)?$" x))))

(defn could-be-boolean?
  [x]
  (if (or (could-be-yes-no? x)
          (could-be-zero-one? x)
          (could-be-true-false? x))
    true
    false))

(defn could-be-longitude?
  [x]
  (if (empty? x)
    true
    (if (try
          (trap/valid-longitude? (Double/parseDouble x))
          (catch Exception _ nil))
      true
      false)))

(defn could-be-latitude?
  [x]
  (if (empty? x)
    true
    (if (try
          (trap/valid-latitude? (Double/parseDouble x))
          (catch Exception _ nil))
      true
      false)))

(defn as-sex
  [v]
  (when (seq v)
    (if (seq (re-matches #"^(?i)M(ale)?$" v))
      "M"
      "F")))

(defn could-be-sex?
  [x]
  (if (or (empty? x)
          (seq (re-matches #"^(?i)M(ale)?|F(emale)?$" x)))
    true
    false))

(defn as-lifestage
  [v]
  (when (seq v)
    (if (seq (re-matches #"^(?i)A(dult)?$" v))
      "adult"
      "juvenile")))

(defn could-be-lifestage?
  [x]
  (if (or (empty? x)
          (seq (re-matches #"^(?i)A(dult)?|J(uvenile)?$" x)))
    true
    false))

(defn could-be-file?
  [x]
  (let [f (file/->file x)]
    (and (file/exists? f)
         (file/readable? f)
         (file/file? f))))

(defn could-be-required?
  [x]
  (if (seq x) true false))

(defn check-possible
  [token checkfn xs]
  (when (every? checkfn xs)
    token))

(defn possible-constraints
  [xs]
  (disj (set
         [(check-possible :required could-be-required? xs)])
        nil))

(defn possible-datatypes
  [xs]
  (disj (set
         [(check-possible :timestamp could-be-timestamp? xs)
          (check-possible :date could-be-date? xs)
          (check-possible :number could-be-number? xs)
          (check-possible :integer could-be-integer? xs)
          (check-possible :readable-integer could-be-readable-integer? xs)
          (check-possible :boolean could-be-yes-no? xs)
          (check-possible :boolean could-be-zero-one? xs)
          (check-possible :boolean could-be-true-false? xs)
          (check-possible :latitude could-be-latitude? xs)
          (check-possible :longitude could-be-longitude? xs)
          (check-possible :sex could-be-sex? xs)
          (check-possible :lifestage could-be-lifestage? xs)
          (check-possible :file could-be-file? xs)
          :string])
        nil))

(defn deserialiser
  [schema]
  (case (or (:validation-type schema)
            (:datatype schema))
    :integer edn/read-string
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
    :string identity))

(defn deserialise
  ([schemas field str-value]
   (if-let [s (get schemas field)]
     (let [f (deserialiser s)]
       (f str-value))))
  ([field str-value]
   (deserialise model/all-mappable-fields field str-value)))