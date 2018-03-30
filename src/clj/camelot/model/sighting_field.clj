(ns camelot.model.sighting-field
  "Additional fields for sighting records."
  (:require
   [schema.core :as s]
   [camelot.util.sighting-fields :as util.sf]
   [camelot.util.db :as db]
   [clj-time.core :as t]))

(def query (db/with-db-keys :sighting-field))

(s/defrecord TSightingField
    [sighting-field-key :- s/Str
     sighting-field-label :- s/Str
     sighting-field-datatype :- s/Keyword
     sighting-field-required :- s/Bool
     sighting-field-default :- s/Str
     sighting-field-affects-independence :- s/Bool
     sighting-field-ordering :- s/Int
     survey-id :- s/Int]
  {s/Any s/Any})

(s/defrecord SightingField
    [sighting-field-id :- s/Int
     sighting-field-created :- org.joda.time.DateTime
     sighting-field-updated :- org.joda.time.DateTime
     sighting-field-key :- s/Str
     sighting-field-label :- s/Str
     sighting-field-datatype :- s/Keyword
     sighting-field-options :- (s/maybe [s/Str])
     sighting-field-default :- (s/maybe s/Str)
     sighting-field-required :- s/Bool
     sighting-field-affects-independence :- s/Bool
     sighting-field-ordering :- s/Int
     survey-id :- s/Int]
  {s/Any s/Any})

(defn sighting-field
  [data]
  (map->SightingField (update data :sighting-field-datatype keyword)))

(def tsighting-field map->TSightingField)

(defn- build-select-tsighting-field
  [{:keys [survey-id internal-key key label ordering]}]
  (tsighting-field {:sighting-field-internal-key internal-key
                    :sighting-field-key key
                    :sighting-field-label label
                    :sighting-field-datatype "select"
                    :sighting-field-required false
                    :sighting-field-default ""
                    :sighting-field-affects-independence true
                    :sighting-field-ordering ordering
                    :survey-id survey-id}))

(defn get-options
  "Return all options for a sighting field."
  [state sf-id]
  (map :sighting-field-option-label (query state :get-options {:sighting-field-id sf-id})))

(defn get-all-options
  "Return all sighting field options keyed by sighting field ID."
  [state]
  (reduce-kv #(assoc %1 %2 (map :sighting-field-option-label %3))
             {} (group-by :sighting-field-id (query state :get-all-options {}))))

(defn- add-options
  "Assoc options for the given field."
  [all-options field]
  (assoc field :sighting-field-options (get all-options (:sighting-field-id field))))

(defn delete-options!
  "Delete options for a sighting field."
  [state field-id]
  (query state :delete-options! {:sighting-field-id field-id})
  nil)

(defn create-options!
  "Create new sighting field options."
  [state sf-id options]
  (doseq [opt options]
    (query state :create-option<! {:sighting-field-id sf-id
                                   :sighting-field-option-label opt})))

(defn get-all
  "Get all sighting fields."
  [state]
  (let [all-options (get-all-options state)]
    (map (comp sighting-field (partial add-options all-options))
         (query state :get-all {}))))

(defn get-specific
  "Return a specific sighting field by field ID."
  [state field-id]
  (if-let [sf (first (query state :get-specific
                            {:sighting-field-id field-id}))]
    (->> (get-options state field-id)
         (assoc sf :sighting-field-options)
         sighting-field)))

(defn update!
  "Update the sighting field with the given ID with `field-config'."
  [state id field-config]
  (db/with-transaction [s state]
    (delete-options! s id)
    (query s :update!
           (assoc (update field-config :sighting-field-datatype name)
                  :sighting-field-id id))
    (when (get-in util.sf/datatypes [(:sighting-field-datatype field-config) :has-options])
      (create-options! s id (:sighting-field-options field-config))))
  (get-specific state id))

(defn create!
  "Create a sighting field with its configuration as `field-config'."
  [state field-config]
  (let [sf (->> (update field-config :sighting-field-datatype name)
                (query state :create<!)
                :1
                int
                (get-specific state))]
    (create-options! state (:sighting-field-id sf) (:sighting-field-options field-config))
    sf))

(defn- create-select-field!
  [state field-config options]
  (create! state (assoc (build-select-tsighting-field field-config)
                        :sighting-field-options options)))

(defn- create-sex-default-field!
  [state survey-id]
  (let [field {:survey-id survey-id
               :key "sex"
               :label "Sex"
               :ordering 5}
        options ["Male" "Female"]]
    (create-select-field! state field options)))

(defn- create-lifestage-default-field!
  [state survey-id]
  (let [field {:survey-id survey-id
               :key "lifestage"
               :label "Life stage"
               :ordering 10}
        options ["Adult" "Juvenile"]]
    (create-select-field! state field options)))

(defn create-default-fields!
  "Create default sighting fields for a survey."
  [state survey-id]
  (create-sex-default-field! state survey-id)
  (create-lifestage-default-field! state survey-id)
  nil)

(defn delete!
  "Delete the sighting field with the given ID."
  [state field-id]
  (query state :delete! {:sighting-field-id field-id})
  nil)
