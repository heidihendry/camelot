(ns camelot.model.library.honey-util
  "HoneySQL wrapper to construct queries.
  The primary purpose of this namespace is to allow construction of partial
  queries, or 'query-parts', which are then transformed into SQL via
  HoneySQL."
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]
   [honeysql.core :as honeysql]
   [honeysql.format :as fmt]
   [honeysql.helpers :as honeyhelpers]
   [camelot.util.model :as model]))

(defmethod fmt/fn-handler "concat" [_ a b]
  (str "(" (fmt/to-sql a) " || " (fmt/to-sql b) ")"))

(defmethod fmt/fn-handler "like" [_ a b]
  (str (fmt/to-sql-value a) " LIKE " (fmt/to-sql-value b)))

(defmethod fmt/fn-handler "not like" [_ a b]
  (str (fmt/to-sql-value a) " NOT LIKE " (fmt/to-sql-value b)))

(defn- ->matchable
  "Transform `sval` to be correctly matched.
  Specifically, account for case and wildcards."
  [sval]
  [:lower (str/replace sval "*" "%")])

(defn ->eq-op
  "Return correct equality operator for `sval`."
  [sval]
  (if (re-find #"\*" sval)
    :like
    :=))

(defn ->neq-op
  "Return correct inequality operator for `sval`."
  [sval]
  (if (re-find #"\*" sval)
    :not-like
    :<>))

(def base-query
  {:select [:trap-station.trap-station-id
            :camera.camera-id
            :trap-station-session.trap-station-session-start-date
            :trap-station-session.trap-station-session-id
            :media.media-capture-timestamp
            :media.media-id]
   :modifiers [:distinct]
   :from [:media]
   :left-join [:trap-station-session-camera
               [:= :trap-station-session-camera.trap-station-session-camera-id
                :media.trap-station-session-camera-id]

               :trap-station-session
               [:= :trap-station-session.trap-station-session-id
                :trap-station-session-camera.trap-station-session-id]

               :trap-station
               [:= :trap-station.trap-station-id
                :trap-station-session.trap-station-id]

               :survey-site
               [:= :survey-site.survey-site-id
                :trap-station.survey-site-id]

               :survey
               [:= :survey.survey-id :survey-site.survey-id]

               :site
               [:= :site.site-id
                :survey-site.site-id]

               :camera
               [:= :camera.camera-id
                :trap-station-session-camera.camera-id]

               :camera-status
               [:= :camera-status.camera-status-id :camera.camera-status-id]

               :sighting
               [:= :sighting.media-id :media.media-id]

               :taxonomy
               [:= :taxonomy.taxonomy-id :sighting.taxonomy-id]

               :species-mass
               [:= :species-mass.species-mass-id :taxonomy.species-mass-id]

               :sighting-field
               [:= :sighting-field.survey-id :survey.survey-id]

               :sighting-field-value
               [:and [:= :sighting-field-value.sighting-field-id :sighting-field.sighting-field-id]
                [:= :sighting-field-value.sighting-id :sighting.sighting-id]]

               :photo [:= :photo.media-id :media.media-id]]
   :order-by [:trap-station.trap-station-id
              :camera.camera-id
              :trap-station-session.trap-station-session-start-date
              :trap-station-session.trap-station-session-id
              :media.media-capture-timestamp]})

(defn- ->negated-field-existence-query-part
  "Produce a partial query to match a field value based on non-existence."
  [field-key sql-expr]
  (if (= (:datatype (get model/schema-definitions field-key)) :integer)
    [:or [:= sql-expr nil]
     [:= sql-expr 0]]
    [:or [:= sql-expr nil]
     [:= sql-expr ""]]))

(defn- ->negated-field-like-query-part
  "Produce a partial query to match a field value based on un-likeness."
  [search-val field-key sql-expr]
  [:not-like [:lower sql-expr] (->matchable search-val)])

(defn- ->negated-field-equality-query-part
  "Produce a partial query to match a field value based on inequality."
  [search-val field-key sql-expr]
  (let [datatype (:datatype (get model/schema-definitions field-key))]
    (condp = datatype
      :integer [:<> sql-expr (edn/read-string search-val)]
      :readable-integer [:<> sql-expr (edn/read-string search-val)]
      :number [:<> sql-expr (edn/read-string search-val)]
      :boolean [:<> sql-expr (edn/read-string search-val)]
      ;; TODO TG-485 https://tree.taiga.io/project/cshclm-camelot/us/485
      :timestamp nil
      :date nil
      [:and
       [:<> [:lower sql-expr] (->matchable search-val)]
       [:<> sql-expr ""]])))

(defn ->negated-field-query-part
  "Produce a partial query to match a field value via negation."
  [^String search-val field-key sql-expr]
  (cond
    (= search-val "*")
    (->negated-field-existence-query-part field-key sql-expr)

    (re-find #"\*" search-val)
    (->negated-field-like-query-part search-val field-key sql-expr)

    :default
    (->negated-field-equality-query-part search-val field-key sql-expr)))

(defn- ->field-existence-query-part
  "Produce a partial query to match a field value based on existence."
  [field-key sql-expr]
  (if (= (:datatype (get model/schema-definitions field-key)) :integer)
    [:and [:<> sql-expr nil]
     [:<> sql-expr 0]]
    [:and [:<> sql-expr nil]
     [:<> sql-expr ""]]))

(defn- ->field-like-query-part
  "Produce a partial query to match a field value based on likeness."
  [search-val field-key sql-expr]
  [:like [:lower sql-expr] (->matchable search-val)])

(defn- ->field-equality-query-part
  "Produce a partial query to match a field value based on equality."
  [search-val field-key sql-expr]
  (let [datatype (:datatype (get model/schema-definitions field-key))]
    (condp = datatype
      :integer [:= sql-expr (edn/read-string search-val)]
      :readable-integer [:= sql-expr (edn/read-string search-val)]
      :number [:= sql-expr (edn/read-string search-val)]
      :boolean [:= sql-expr (edn/read-string search-val)]
      ;; TODO TG-485 https://tree.taiga.io/project/cshclm-camelot/us/485
      :timestamp nil
      :date nil
      [:= [:lower sql-expr] (->matchable search-val)])))

(defn ->field-query-part
  "Produce a partial query to match a field value."
  [^String search-val field-key sql-expr]
  (cond
    (= search-val "*")
    (->field-existence-query-part field-key sql-expr)

    (re-find #"\*" search-val)
    (->field-like-query-part search-val field-key sql-expr)

    :default
    (->field-equality-query-part search-val field-key sql-expr)))

(defn ->negated-full-text-query-part
  "Full text search with negation.
  Note, this may not work as expected, as the query will match any one of
  multiple sightings or sighting-fields. For example, '!Adult' will match
  against a lifestage."
  [search]
  (let [sval (:value search)]
    (vec
     (concat
      [:and
       [:or
        [:= :taxonomy.taxonomy-genus nil]
        [:= :taxonomy.taxonomy-species nil]
        [(->neq-op sval) [:lower [:concat [:concat :taxonomy.taxonomy-genus [:cast " " :char]]
                                  :taxonomy.taxonomy-species]]
         (->matchable sval)]]]
      (vec (conj
            (map (fn [k]
                   [:or [:= k nil]
                    [(->neq-op sval) [:lower k] (->matchable sval)]])
                 (model/qualified-searchable-field-keys))
            [:or
             [:= :sighting-field-value.sighting-field-value-data nil]
             [(->neq-op sval) [:lower :sighting-field-value.sighting-field-value-data]
              (->matchable sval)]]))))))

(defn ->normal-full-text-query-part
  "Full text search.
  Strictly speaking, this is not a *full* full text search, but creates a
  partial query for searching full-text fields, as defined by our model."
  [search]
  (let [sval (:value search)]
    (vec
     (concat
      [:or
       [(->eq-op sval) [:lower [:concat [:concat :taxonomy.taxonomy-genus [:cast " " :char]]
                                :taxonomy.taxonomy-species]]
        (->matchable sval)]]
      (vec (conj
            (map (fn [k]
                   [(->eq-op sval) [:lower k] (->matchable sval)])
                 (model/qualified-searchable-field-keys))
            [(->eq-op sval) [:lower :sighting-field-value.sighting-field-value-data]
             (->matchable sval)]))))))

(defn ->qualified-field-query-part
  "Return a partial query identifying the requested field."
  [search]
  (cond
    (= (:field search) :taxonomy-label)
    [:lower [:concat [:concat :taxonomy.taxonomy-genus [:cast " " :char]]
             :taxonomy.taxonomy-species]]

    (:sighting-field? search)
    (keyword (str (name (:field search)) "-value.sighting-field-value-data"))

    :default
    (keyword (str (name (:table search)) "." (name (:field search))))))

(defn ->sighting-field-query-part
  "Add a partial query to match a given field-key."
  [field-key]
  (let [field (name field-key)]
    [[:sighting-field field-key]
     [:and [:= (keyword (str field ".survey-id")) :survey.survey-id]
      [:= (keyword (str field ".sighting-field-key"))
       (str/replace field #"^field-" "")]]

     [:sighting-field-value (keyword (str field "-value"))]
     [:and [:= (keyword (str field "-value.sighting-id")) :sighting.sighting-id]
      [:= (keyword (str field "-value.sighting-field-id"))
       (keyword (str field ".sighting-field-id"))]]]))

(defn ->query
  "Given a query and an optional partial query defining the WHERE, produce the
  SQL to fetch the desired records."
  [query where]
  (let [merged-query (if where
                       (honeyhelpers/where query where)
                       query)]
    (honeysql/format {:select [:result.media_id]
                      :from [[merged-query :result]]})))
