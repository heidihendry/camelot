(require '[yesql.core :as sql])
(require '[clojure.java.jdbc :as jdbc])
(require '[clj-time.core :as t])
(require '[clj-time.coerce :as tc])
(require '[clojure.string :as cstr])

(sql/defqueries "sql/migration-helpers/040.sql")

(defn- build-field-descriptor
  [{:keys [survey-id internal-key key label ordering]}]
  {:sighting_field_internal_key internal-key
   :sighting_field_key key
   :sighting_field_label label
   :sighting_field_datatype "select"
   :sighting_field_required false
   :sighting_field_default ""
   :sighting_field_affects_independence true
   :sighting_field_ordering ordering
   :survey_id survey-id
   :current_timestamp (tc/to-long (t/now))})

(defn- -m040-create-field!
  [conn config options]
  (let [record (-create-internal-sighting-field<! config conn)
        field-id (int (:1 record))]
    (doseq [o options]
      (-create-option<! {:sighting_field_option_label o
                         :sighting_field_id field-id
                         :current_timestamp (tc/to-long (t/now))}
                        conn))
    field-id))

(defn- -m040-create-sex-field!
  [conn survey-id]
  (let [conf {:survey-id survey-id
              :internal-key "sighting-sex"
              :key "sex"
              :label "Sex"
              :ordering 5}]
    (-m040-create-field! conn (build-field-descriptor conf)
                         ["Male" "Female"])))

(defn- -m040-create-lifestage-field!
  [conn survey-id]
  (let [conf {:survey-id survey-id
              :internal-key "sighting-lifestage"
              :key "lifestage"
              :label "Life stage"
              :ordering 10}]
    (-m040-create-field! conn (build-field-descriptor conf)
                         ["Adult" "Juvenile"])))

(defn- -m040-create-field-value!
  [conn sighting-id field-id value]
  (-create-field-value<! {:sighting_field_value_data value
                          :sighting_id sighting-id
                          :sighting_field_id field-id
                          :current_timestamp (tc/to-long (t/now))}
                         conn))

(defn- -m040-migrate-sighting-fields!
  [conn sightings field-id field-accessor]
  (doseq [s sightings]
    (when-let [value (field-accessor s)]
      (-m040-create-field-value!
       conn
       (:sighting_id s)
       field-id
       value))))

(defn- -m040-convert-sex-field-representation
  [sightings]
  (condp = (:sighting_sex sightings)
    "M" "Male"
    "F" "Female"
    nil))

(defn- -m040-migrate-sex-fields!
  [conn sightings field-id]
  (-m040-migrate-sighting-fields! conn sightings field-id -m040-convert-sex-field-representation))

(defn- -m040-migrate-lifestage-fields!
  [conn sightings field-id]
  (-m040-migrate-sighting-fields! conn sightings field-id
                                  #(when-let [v (:sighting_lifestage %)]
                                     (cstr/capitalize v))))

(defn- -m040-migrate-survey-data
  [conn survey-id]
  (let [sightings (-get-sightings-for-survey {:survey_id survey-id} conn)]
    (->> (-m040-create-sex-field! conn survey-id)
         (-m040-migrate-sex-fields! conn sightings))
    (->> (-m040-create-lifestage-field! conn survey-id)
         (-m040-migrate-lifestage-fields! conn sightings))))

(defn- -m040-get-survey-ids
  [conn]
  (map :survey_id (-get-survey-ids {} conn)))

(defn- -m040-upgrade
  [conn]
  (jdbc/with-db-transaction [tx conn]
    (let [conn {:connection tx}]
      (doseq [survey (-m040-get-survey-ids conn)]
        (-m040-migrate-survey-data conn survey)))))

(-m040-upgrade camelot.migration/*connection*)
