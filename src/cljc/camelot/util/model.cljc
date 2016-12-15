(ns camelot.util.model
)

(def schema-definitions
  {:camera-id {:datatype :integer
               :required true
               :unmappable true}
   :camera-make {:datatype :string
                 :required false
                 :order 21}
   :camera-model {:datatype :string
                  :required false
                  :order 20}
   :camera-name {:datatype :string
                 :required true
                 :order 10}
   :media-attention-needed {:datatype :boolean
                            :required false
                            :order 80}
   :media-cameracheck {:datatype :boolean
                       :required false
                       :order 82}
   :media-capture-timestamp {:datatype :timestamp
                             :required true
                             :order 30}
   :media-created {:datatype :timestamp
                   :required true
                   :unmappable true}
   :media-filename {:datatype :string
                    :required true
                    :unmappable true}
   :media-format {:datatype :string
                  :required true
                  :unmappable true}
   :media-id {:datatype :integer
              :required true
              :unmappable true}
   :media-processed {:datatype :boolean
                     :required false
                     :order 81}
   :media-updated {:datatype :timestamp
                   :required true
                   :unmappable true}
   :media-uri {:datatype :string
               :required false
               :unmappable true}
   :sighting-id {:datatype :integer
                 :required true
                 :unmappable true}
   :sighting-created {:datatype :timestamp
                      :required true
                      :unmappable true}
   :sighting-quantity {:datatype :integer
                       :required false
                       :order 40}
   :sighting-lifestage {:datatype :string
                        :validation-type :lifestage
                        :required false
                        :order 42}
   :sighting-sex {:datatype :string
                  :validation-type :sex
                  :required false
                  :order 41}
   :sighting-updated {:datatype :timestamp
                      :required true
                      :unmappable true}
   :site-city {:datatype :string
               :required false
               :order 5}
   :site-id {:datatype :integer
             :required true
             :unmappable true}
   :site-name {:datatype :string
               :required true
               :order 3}
   :site-sublocation {:datatype :string
                      :required false
                      :order 4}
   :site-state-province {:datatype :string
                         :required false
                         :order 6}
   :site-country {:datatype :string
                  :required false
                  :order 7}
   :survey-id {:datatype :integer
               :required true
               :unmappable true}
   :survey-name {:datatype :string
                 :required true
                 :unmappable true}
   :survey-site-id {:datatype :integer
                    :required true
                    :unmappable true}
   :taxonomy-class {:datatype :string
                    :required false
                    :order 64}
   :taxonomy-created {:datatype :timestamp
                      :required true
                      :unmappable true}
   :taxonomy-common-name {:datatype :string
                          :required false
                          :order 60}
   :taxonomy-family {:datatype :string
                     :required false
                     :order 63}
   :taxonomy-genus {:datatype :string
                    :required false
                    :order 62}
   :taxonomy-id {:datatype :integer
                 :required true
                 :unmappable true}
   :taxonomy-label {:datatype :string
                    :required true
                    :unmappable true}
   :taxonomy-notes {:datatype :string
                    :required false
                    :order 66}
   :taxonomy-order {:datatype :string
                    :required false
                    :order 65}
   :taxonomy-species {:datatype :string
                      :required false
                      :order 61}
   :taxonomy-updated {:datatype :timestamp
                      :required true
                      :unmappable true}
   :trap-station-id {:datatype :integer
                     :required true
                     :unmappable true}
   :trap-station-latitude {:datatype :number
                           :validation-type :latitude
                           :required true
                           :order 30}
   :trap-station-longitude {:datatype :number
                            :validation-type :longitude
                            :required true
                            :order 31}
   :trap-station-name {:datatype :string
                       :required true
                       :order 15}
   :trap-station-session-camera-id {:datatype :integer
                                    :required true
                                    :unmappable true}
   :trap-station-session-id {:datatype :integer
                             :required true
                             :unmappable true}
   :trap-station-session-start-date {:datatype :timestamp
                                     :required true
                                     :order 20}
   :trap-station-session-end-date {:datatype :timestamp
                                   :required true
                                   :order 22}
   :photo-fnumber-setting {:datatype :string
                           :required false
                           :order 70}
   :photo-exposure-value {:datatype :string
                          :required false
                          :order 71}
   :photo-flash-setting {:datatype :string
                         :required false
                         :order 72}
   :photo-focal-setting {:datatype :string
                         :required false
                         :order 73}
   :photo-iso-setting {:datatype :integer
                       :required false
                       :order 74}
   :photo-orientation {:datatype :string
                       :required false
                       :order 75}
   :photo-resolution-x {:datatype :string
                        :required false
                        :order 76}
   :photo-resolution-y {:datatype :string
                        :required false
                        :order 77}})

(def absolute-path {:absolute-path {:datatype :file
                                    :required true
                                    :order 40}})

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

(defn mapping-validation-problems
  [schemas column calculated-schema]
  (let [s (get schemas column)]
    (if s
      (->> {}
           (maybe-datatype-problem s calculated-schema)
           (maybe-required-constraint-problem s calculated-schema))
      {:global ::schema-not-found})))

(defn describe-datatype
  [schema translation-fn]
  (translation-fn (case (or (:validation-type schema)
                            (:datatype schema))
                    :integer ::datatype-integer
                    :number ::datatype-number
                    :sex ::datatype-sex
                    :lifestage ::datatype-lifestage
                    :timestamp ::datatype-timestamp
                    :longitude ::datatype-longitude
                    :latitude ::datatype-latitude
                    :boolean ::datatype-boolean
                    :file ::datatype-file
                    :string ::datatype-string)))

(defn reason-mapping-invalid
  [schemas column calculated-schema translation-fn]
  (let [ps (mapping-validation-problems schemas column calculated-schema)]
    (if (:global ps)
      (translation-fn (:global ps) column)
      (cond
        (nil? calculated-schema)
        (translation-fn ::calculated-schema-not-available
                        column)

        (and (:required-constraint ps) (:datatype ps))
        (translation-fn ::datatype-and-required-constraint-problem
                        (describe-datatype  (:datatype ps) translation-fn))

        (and (:datatype ps))
        (translation-fn ::datatype-problem-only
                        (describe-datatype (:datatype ps) translation-fn))

        (and (:required-constraint ps))
        (translation-fn ::required-constraint-problem-only)))))

(defn check-mapping
  "Validate all mappings, returning a list of invalid mappings."
  ([schemas mappings calculated-schema translation-fn]
   (let [xform (comp (map (fn [[k v]]
                            (let [r (get calculated-schema (get mappings k))]
                              (reason-mapping-invalid schemas k r translation-fn))))
                     (remove nil?))]
     (sequence xform schemas)))
  ([mappings calculated-schema translation-fn]
   (check-mapping all-mappable-fields
                  mappings calculated-schema translation-fn)))

(def fields (keys schema-definitions))