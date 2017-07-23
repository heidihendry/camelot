(ns camelot.model.sighting-field-value-test
  (:require [camelot.model.sighting-field-value :as sut]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [camelot.test-util.state :as state]
            [camelot.util.db :as db]))

(def ^:dynamic *get-for-sighting*
  [{:sighting-field-value-id 2
    :sighting-field-value-created (t/date-time 2017 1 1 20 30 40)
    :sighting-field-value-updated (t/date-time 2017 1 1 20 30 40)
    :sighting-field-value-data "Value"
    :sighting-field-id 5
    :sighting-id 6}])

(defn test-get-for-sighting
  [ps]
  *get-for-sighting*)

(def ^:dynamic *create<!*
  {:1 1})

(defn test-create<!
  [ps]
  *create<!*)

(def ^:dynamic *get-specific*
  [{:sighting-field-value-id 1
    :sighting-field-value-created (t/date-time 2017 1 1 20 30 40)
    :sighting-field-value-updated (t/date-time 2017 1 1 20 30 40)
    :sighting-field-value-data "Value"
    :sighting-field-id 2
    :sighting-id 3}])

(defn test-get-specific
  [ps]
  *get-specific*)

(defn test-update!
  [ps]
  nil)

(def ^:dynamic *arguments* nil)

(defmacro with-args [[args] & body]
  `(let [ts# (transient {})
         ~args ts#]
     (binding [*arguments* ts#]
       ~@body)))

(defn update-for-sighting!
  [state sighting-id data]
  (with-redefs
    [db/fn-with-db-keys
     (fn [s f ps]
       (let [fname (:name (meta f))]
         (assoc! *arguments* fname
                 (conj (or (get *arguments* fname) []) ps))
         (eval (list (symbol (str (ns-name *ns*)) (str "test" fname)) ps))))]
    (sut/update-for-sighting! state sighting-id data)))

(deftest test-update-for-sighting
  (testing "update-for-sighting"
    (testing "should create new record if existing value not found"
      (with-args [args]
        (update-for-sighting! (state/gen-state) 3 {1 "value"})
        (is (= (get args '-create<!)
               [{:sighting-field-id 1
                 :sighting-field-value-data "value"
                 :sighting-id 3}]))))

    (testing "should update record if existing value is found"
      (with-args [args]
        (update-for-sighting! (state/gen-state) 6 {5 "Updated value"})
        (is (= (get args '-update!)
               [{:sighting-field-value-id 2
                 :sighting-field-value-data "Updated value"}]))))

    (testing "should mix updates and creates"
      (with-args [args]
        (update-for-sighting! (state/gen-state) 6 {5 "Updated value"
                                                   1 "Create value"})
        (is (= (get args '-update!)
               [{:sighting-field-value-id 2
                 :sighting-field-value-data "Updated value"}]))
        (is (= (get args '-create<!)
               [{:sighting-field-id 1
                 :sighting-field-value-data "Create value"
                 :sighting-id 6}]))))

    (testing "should not create or update a field if no change needed"
      (with-args [args]
        (update-for-sighting! (state/gen-state) 6 {5 "Value"})
        (is (= (get args '-update!) nil))
        (is (= (get args '-create<!) nil))))))
