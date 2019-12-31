(ns camelot.util.model
  (:require
   [camelot.util.sighting-fields :as sighting-fields]))

(def schema-definitions
  {:camera-id {:datatype :integer
               :required true
               :table :camera
               :unmappable true}
   :camera-make {:datatype :string
                 :max-length 31
                 :required false
                 :full-text-search true
                 :table :camera
                 :order 20}
   :camera-model {:datatype :string
                  :max-length 31
                  :required false
                  :full-text-search true
                  :table :camera
                  :order 21}
   :camera-name {:datatype :string
                 :max-length 31
                 :required true
                 :full-text-search true
                 :table :camera
                 :order 10}
   :camera-notes {:datatype :string
                  :required false
                  :order 22
                  :table :camera
                  :export-excluded true}
   :media-attention-needed {:datatype :boolean
                            :required false
                            :table :media
                            :order 80}
   :media-cameracheck {:datatype :boolean
                       :required false
                       :table :media
                       :order 82}
   :media-capture-timestamp {:datatype :timestamp
                             :required true
                             :table :media
                             :order 30}
   :media-created {:datatype :timestamp
                   :required true
                   :table :media
                   :unmappable true}
   :media-filename {:datatype :string
                    :required true
                    :table :media
                    :unmappable true}
   :media-format {:datatype :string
                  :required true
                  :table :media
                  :unmappable true}
   :media-id {:datatype :integer
              :required true
              :table :media
              :unmappable true}
   :media-processed {:datatype :boolean
                     :required false
                     :table :media
                     :order 81}
   :media-reference-quality {:datatype :boolean
                             :required false
                             :table :media
                             :order 82}
   :media-updated {:datatype :timestamp
                   :required true
                   :table :media
                   :unmappable true}
   :media-uri {:datatype :string
               :required false
               :calculated true
               :unmappable true}
   :media-notes {:datatype :string
                 :required false
                 :order 83
                 :unmappable true
                 :table :media
                 :export-excluded true}
   :sighting-id {:datatype :integer
                 :required true
                 :table :sighting
                 :unmappable true}
   :sighting-created {:datatype :timestamp
                      :required true
                      :table :sighting
                      :unmappable true}
   :sighting-quantity {:datatype :integer
                       :required false
                       :table :sighting
                       :order 40}
   :sighting-updated {:datatype :timestamp
                      :required true
                      :table :sighting
                      :unmappable true}
   :site-city {:datatype :string
               :required false
               :full-text-search true
               :table :site
               :order 5}
   :site-id {:datatype :integer
             :required true
             :table :site
             :unmappable true}
   :site-name {:datatype :string
               :max-length 127
               :required true
               :full-text-search true
               :table :site
               :order 3}
   :site-sublocation {:datatype :string
                      :max-length 127
                      :required false
                      :full-text-search true
                      :table :site
                      :order 4}
   :site-state-province {:datatype :string
                         :max-length 127
                         :required false
                         :full-text-search true
                         :table :site
                         :order 6}
   :site-country {:datatype :string
                  :max-length 127
                  :required false
                  :full-text-search true
                  :table :site
                  :order 7}
   :site-area {:datatype :number
               :required false
               :table :site
               :order 8}
   :site-notes {:datatype :string
                :required false
                :order 9
                :export-excluded true}
   :survey-id {:datatype :integer
               :required true
               :table :survey
               :unmappable true}
   :survey-name {:datatype :string
                 :required true
                 :full-text-search true
                 :table :survey
                 :unmappable true}
   :survey-notes {:datatype :string
                  :required true
                  :unmappable true
                  :table :survey
                  :export-excluded true}
   :survey-created {:datatype :timestamp
                    :required true
                    :table :survey
                    :unmappable true}
   :survey-updated {:datatype :timestamp
                    :required true
                    :table :survey
                    :unmappable true}
   :survey-site-id {:datatype :integer
                    :required true
                    :table :survey-site
                    :unmappable true}
   :taxonomy-created {:datatype :timestamp
                      :required true
                      :table :taxonomy
                      :unmappable true}
   :taxonomy-common-name {:datatype :string
                          :max-length 255
                          :required false
                          :full-text-search true
                          :table :taxonomy
                          :order 60}
   :taxonomy-family {:datatype :string
                     :max-length 255
                     :required false
                     :full-text-search true
                     :table :taxonomy
                     :order 63}
   :taxonomy-genus {:datatype :string
                    :max-length 255
                    :required false
                    :full-text-search true
                    :table :taxonomy
                    :order 62}
   :taxonomy-id {:datatype :integer
                 :required true
                 :table :taxonomy
                 :unmappable true}
   :taxonomy-label {:datatype :string
                    :required true
                    :calculated true
                    :unmappable true}
   :taxonomy-notes {:datatype :string
                    :required false
                    :order 66
                    :table :taxonomy
                    :export-excluded true}
   :taxonomy-class {:datatype :string
                    :max-length 255
                    :required false
                    :full-text-search true
                    :table :taxonomy
                    :order 65}
   :taxonomy-order {:datatype :string
                    :max-length 255
                    :required false
                    :full-text-search true
                    :table :taxonomy
                    :order 64}
   :taxonomy-species {:datatype :string
                      :max-length 255
                      :required false
                      :full-text-search true
                      :table :taxonomy
                      :order 61}
   :taxonomy-updated {:datatype :timestamp
                      :required true
                      :table :taxonomy
                      :unmappable true}
   :species-mass-id {:datatype :integer
                     :required false
                     :table :species-mass
                     :unmappable true}
   :species-mass-start {:datatype :number
                        :required false
                        :table :species-mass
                        :unmappable true}
   :species-mass-end {:datatype :number
                      :required false
                      :table :species-mass
                      :unmappable true}
   :trap-station-id {:datatype :integer
                     :required true
                     :table :trap-station
                     :unmappable true}
   :trap-station-latitude {:datatype :number
                           :validation-type :latitude
                           :required true
                           :table :trap-station
                           :order 30}
   :trap-station-longitude {:datatype :number
                            :validation-type :longitude
                            :required true
                            :table :trap-station
                            :order 31}
   :trap-station-altitude {:datatype :readable-integer
                           :required false
                           :table :trap-station
                           :order 16}
   :trap-station-notes {:datatype :string
                        :required false
                        :order 17
                        :table :trap-station
                        :export-excluded true}
   :trap-station-name {:datatype :string
                       :max-length 255
                       :required true
                       :full-text-search true
                       :table :trap-station
                       :order 15}
   :trap-station-session-camera-id {:datatype :integer
                                    :required true
                                    :table :trap-station-session-camera
                                    :unmappable true}
   :trap-station-session-id {:datatype :integer
                             :required true
                             :table :trap-station-session
                             :unmappable true}
   :trap-station-session-start-date {:datatype :date
                                     :required true
                                     :table :trap-station-session
                                     :order 20}
   :trap-station-session-end-date {:datatype :date
                                   :required true
                                   :table :trap-station-session
                                   :order 22}
   :photo-fnumber-setting {:datatype :string
                           :max-length 15
                           :required false
                           :table :photo
                           :order 70}
   :photo-exposure-value {:datatype :string
                          :required false
                          :table :photo
                          :order 71}
   :photo-flash-setting {:datatype :string
                         :max-length 255
                         :required false
                         :table :photo
                         :order 72}
   :photo-focal-length {:datatype :string
                        :max-length 15
                        :required false
                        :table :photo
                        :order 73}
   :photo-iso-setting {:datatype :integer
                       :required false
                       :table :photo
                       :order 74}
   :photo-orientation {:datatype :string
                       :max-length 127
                       :required false
                       :table :photo
                       :order 75}
   :photo-resolution-x {:datatype :readable-integer
                        :required false
                        :table :photo
                        :order 76}
   :photo-resolution-y {:datatype :readable-integer
                        :required false
                        :table :photo
                        :order 77}
   :suggestion-id {:datatype :integer
                   :required true
                   :table :suggestion
                   :unmappable true}
   :suggestion-key {:datatype :string
                    :table :suggestion
                    :unmappable true}})

(def absolute-path {:absolute-path {:datatype :file
                                    :required true
                                    :order 40}})

(defn qualified-searchable-field-keys
  []
  (let [xform (comp (filter (fn [[k v]] (:full-text-search v)))
                    (map (fn [[k v]]
                           (keyword (str (name (:table v)) "." (name k))))))]
    (sequence xform (into [] schema-definitions))))

(defn sighting-field-to-schema-definition
  [field]
  (let [k (keyword (str "field-" (:sighting-field-key field)))
        dt (get-in sighting-fields/datatypes [(:sighting-field-datatype field)
                                              :deserialiser-datatype])]
    [k {:datatype dt
        :required (:sighting-field-required field)
        :label (:sighting-field-label field)
        :order (+ 100 (:sighting-field-ordering field))}]))

(defn with-sighting-fields
  [xs fields]
  (concat xs (map sighting-field-to-schema-definition fields)))

(def extended-schema-definitions
  (merge schema-definitions absolute-path))

(defn with-absolute-path
  [xs]
  (conj xs (first (vec absolute-path))))

(defn mappable-fields
  [sd]
  (remove #(:unmappable (second %)) sd))

(def all-mappable-fields
  "All fields which a user may mapped to."
  (into {} (-> schema-definitions
               mappable-fields
               with-absolute-path)))

(defn required-fields
  [sd]
  (filter #(:required (second %)) sd))

(defn optional-fields
  [sd]
  (remove #(:required (second %)) sd))

(defn maybe-datatype-problem
  [schema calculated-constraints results]
  (if (not-any? #{(or (:validation-type schema) (:datatype schema))}
                (:datatypes calculated-constraints))
    (assoc results :datatype schema)
    results))

(defn maybe-required-constraint-problem
  [schema calculated-constraints results]
  (if (and (:required schema)
           (not-any? #{:required} (:constraints calculated-constraints)))
    (assoc results :required-constraint true)
    results))

(defn maybe-max-length-problem
  [schema calculated-constraints results]
  (if (and (number? (:max-length schema))
           (number? (:max-length calculated-constraints))
           (> (:max-length calculated-constraints)
              (:max-length schema)))
    (assoc results :max-length (:max-length schema))
    results))

(defn mapping-validation-problems
  [schemas column calculated-schema]
  (let [s (get schemas column)]
    (if s
      (->> {}
           (maybe-datatype-problem s calculated-schema)
           (maybe-required-constraint-problem s calculated-schema)
           (maybe-max-length-problem s calculated-schema))
      {:global ::schema-not-found})))

(defn describe-datatype
  [schema translation-fn]
  (translation-fn (case (or (:validation-type schema)
                            (:datatype schema))
                    :integer ::datatype-integer
                    :readable-integer ::datatype-integer
                    :number ::datatype-number
                    :timestamp ::datatype-timestamp
                    :longitude ::datatype-longitude
                    :latitude ::datatype-latitude
                    :boolean ::datatype-boolean
                    :file ::datatype-file
                    :date ::datatype-date
                    :string ::datatype-string)))

(defn known-field?
  "Returns `true` if `field-key` is in the schema. False otherwise."
  [field-key]
  (contains? schema-definitions field-key))

(defn reason-mapping-invalid
  [schemas column calculated-schema translation-fn]
  (let [ps (mapping-validation-problems schemas column calculated-schema)]
    (if (:global ps)
      (translation-fn (:global ps) column)
      (cond
        (nil? calculated-schema)
        (translation-fn ::calculated-schema-not-available column)

        (and (:required-constraint ps) (:datatype ps))
        (translation-fn ::datatype-and-required-constraint-problem
                        (describe-datatype  (:datatype ps) translation-fn))

        (:datatype ps)
        (translation-fn ::datatype-problem-only
                        (describe-datatype (:datatype ps) translation-fn))

        (:required-constraint ps)
        (translation-fn ::required-constraint-problem-only)

        (:max-length ps)
        (translation-fn ::max-length-problem
                        (:max-length ps)
                        (:max-length calculated-schema))))))

(defn check-mapping
  "Validate all mappings, returning a list of invalid mappings."
  ([schemas mappings calculated-schema translation-fn]
   (let [xform (comp (map (fn [[k v]]
                            (if-let [m (get mappings k)]
                              (let [r (get calculated-schema m)]
                                (reason-mapping-invalid schemas k r translation-fn)))))
                     (remove nil?))]
     (sequence xform schemas)))
  ([mappings calculated-schema translation-fn]
   (check-mapping all-mappable-fields
                  mappings calculated-schema translation-fn)))

(defn effective-datatype
  "Return the effective datatype given the schema."
  [schema]
  (or (:validation-type schema) (:datatype schema)))

(def fields (keys schema-definitions))
