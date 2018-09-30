(ns camelot.library.search
  "Media search for the library.
  Search is performed via transforming camelot search expressions into SQL."
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [camelot.library.honey-util :as honey-util]
   [camelot.util.db :as db-util]
   [camelot.util.model :as model]))

(defn- full-text-search
  "Build a partial query matching numerous fields."
  [search]
  (if (:negated? search)
    (honey-util/->negated-full-text-query-part search)
    (honey-util/->normal-full-text-query-part search)))

(defn- field-search
  "Build a partial query to match a specific field."
  [search]
  (let [field-expr (honey-util/->qualified-field-query-part search)]
    (if (:negated? search)
      (honey-util/->negated-field-query-part search field-expr)
      (honey-util/->field-query-part search field-expr))))

(defn- search-field-known?
  "Returns `true` if `field-key` is a sighting-field or a field in the schema.
  False otherwise."
  [search]
  (or (:sighting-field? search)
      (model/known-field? (:field search))))

(defn- query-part
  "Produce a partial query for the given search expression.
  May return nil if given an invalid field."
  [search]
  (if (:field search)
    (when (search-field-known? search)
      (field-search search))
    (full-text-search search)))

(defn- conjunctive-query-parts
  "Return conjunctive query parts.
  Any failed search causes entire conjunctive expression to not match."
  [search]
  (let [qpart (map query-part search)]
    (if (seq (filter nil? qpart))
      []
      (vec (cons :and qpart)))))

(defn- disjunctive-query-parts
  "Return vector of disjunctive query parts."
  [search]
  (let [qpart (filter seq (map conjunctive-query-parts search))]
    (if (seq qpart)
      (vec (cons :or qpart))
      [])))

(defn- where-query
  "Return an object indicating search validity.
  If a non-empty search does not produce a query, it is deemed invalid."
  [psearch]
  (if (empty? psearch)
    {:valid true :where nil}
    (let [qpart (disjunctive-query-parts psearch)]
      (if (seq qpart)
        {:valid true :where qpart}
        {:valid false}))))

(defn- sighting-field-join-reducer
  "Add a condition for a sighting-field in the parsed search."
  [acc psearch]
  (let [field-key (:field psearch)]
    (if (contains? (:added-fields acc) field-key)
      acc
      (-> acc
          (update :result
                  #(concat % (honey-util/->sighting-field-query-part psearch)))
          (update :added-fields #(conj % field-key))))))

(defn- join-sighting-fields
  "Add a join for each sighting-field in `psearch`."
  [q psearch]
  (update q :left-join
          #(let [init-acc {:result % :added-fields #{}}]
             (->> psearch
                  flatten
                  (filter :sighting-field?)
                  (reduce sighting-field-join-reducer init-acc)
                  :result
                  vec))))

(defn media
  "Return the IDs for all media matching `psearch`."
  [state psearch]
  (try
    (let [{valid :valid where-qpart :where} (where-query psearch)]
      (if valid
        (let [ext-q (join-sighting-fields honey-util/base-query psearch)
              sql (honey-util/->query ext-q where-qpart)]
          (map :media_id (jdbc/query (get-in state [:database :connection]) sql)))
        '()))
    (catch Exception e
      (log/error (.getMessage e))
      (doseq [st (.getStackTrace e)]
        (log/error (.toString st)))
      '())))
