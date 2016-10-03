(ns camelot.util.model)

(def schema-definitions
  {:camera-id {:datatype :integer
               :required true
               :unmappable true}
   :camera-make {:datatype :string
                 :required false}
   :camera-model {:datatype :string
                  :required false}
   :camera-name {:datatype :string
                 :required true}
   :media-attention-needed {:datatype :boolean
                            :required false}
   :media-cameracheck {:datatype :boolean
                       :required false}
   :media-capture-timestamp {:datatype :timestamp
                             :required true}
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
                     :required false}
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
                       :required false}
   :sighting-lifestage {:datatype :string
                        :validation-type :lifestage
                        :required false}
   :sighting-sex {:datatype :string
                  :validation-type :sex
                  :required false}
   :sighting-updated {:datatype :timestamp
                      :required true
                      :unmappable true}
   :site-city {:datatype :string
               :required false}
   :site-id {:datatype :integer
             :required true
             :unmappable true}
   :site-name {:datatype :string
               :required true}
   :site-sublocation {:datatype :string
                      :required false}
   :site-state-province {:datatype :string
                         :required false}
   :site-country {:datatype :string
                  :required false}
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
                    :required false}
   :taxonomy-created {:datatype :timestamp
                      :required true
                      :unmappable true}
   :taxonomy-common-name {:datatype :string
                          :required false}
   :taxonomy-family {:datatype :string
                     :required false}
   :taxonomy-genus {:datatype :string
                    :required false}
   :taxonomy-id {:datatype :integer
                 :required true
                 :unmappable true}
   :taxonomy-label {:datatype :string
                    :required true
                    :unmappable true}
   :taxonomy-notes {:datatype :string
                    :required false}
   :taxonomy-order {:datatype :string
                    :required false}
   :taxonomy-species {:datatype :string
                      :required false}
   :taxonomy-updated {:datatype :timestamp
                      :required true
                      :unmappable true}
   :trap-station-id {:datatype :integer
                     :required true
                     :unmappable true}
   :trap-station-latitude {:datatype :number
                           :validation-type :latitude
                           :required true}
   :trap-station-longitude {:datatype :number
                            :validation-type :longitude
                            :required true}
   :trap-station-name {:datatype :string
                       :required true}
   :trap-station-session-camera-id {:datatype :integer
                                    :required true
                                    :unmappable true}
   :trap-station-session-id {:datatype :integer
                             :required true
                             :unmappable true}
   :trap-station-session-start-date {:datatype :timestamp
                                     :required true}
   :trap-station-session-end-date {:datatype :timestamp
                                   :required false}})

(defn mappable-fields
  [sd]
  (remove #(:unmappable (second %)) sd))

(defn required-fields
  [sd]
  (filter #(:required (second %)) sd))

(defn optional-fields
  [sd]
  (remove #(:required (second %)) sd))

(defn maybe-datatype-problem
  [s calculated-constraints results]
  (if (not-any? #{(or (:validation-type s) (:datatype s))}
                (:datatypes calculated-constraints))
    (assoc results :datatype s)
    results))

(defn maybe-required-constraint-problem
  [s calculated-constraints results]
  (if (and (:required s)
           (not-any? #{:required} (:constraints calculated-constraints)))
    (assoc results :required-constraint true)
    results))

(defn mapping-validation-problems
  [column calculated-schema]
  (let [s (schema-definitions column)]
    (if s
      (-> {}
          (partial maybe-datatype-problem s calculated-schema)
          (partial maybe-required-constraint-problem s calculated-schema))
      {:global ::schema-not-found})))

(defn describe-datatype
  [translation-fn dt]
  (translation-fn (case dt
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
  [column calculated-schema translation-fn]
  (let [ps (mapping-validation-problems column calculated-schema)]
    (if (:global ps)
      (translation-fn (:global ps) column)
      (cond
        (and (:required-constraint ps) (:datatype ps))
        (translation-fn ::datatype-and-required-constraint-problem
                        (describe-datatype translation-fn (:datatype ps)))

        (and (:datatype ps))
        (translation-fn ::datatype-problem-only
                        (describe-datatype translation-fn (:datatype ps)))

        (and (:required-constraint ps))
        (translation-fn ::required-constraint-problem-only)))))

(def fields (keys schema-definitions))
