(require '[yesql.core :as sql])
(require '[clojure.string :as str])
(require '[camelot.util.state :as state])
(require '[clojure.java.jdbc :as jdbc])
(require '[camelot.util.db :as db])

(sql/defqueries "sql/migration-helpers/021.sql")
(sql/defqueries "sql/migration-helpers/db.sql")

(defn- -m021-create-taxonomy
  [spp]
  (let [name-parts (str/split (:species_scientific_name spp) #" ")]
    {:taxonomy_notes (:species_notes spp)
     :taxonomy_created (:species_created spp)
     :taxonomy_updated (:species_updated spp)
     :taxonomy_common_name (:species_common_name spp)
     :taxonomy_class nil
     :taxonomy_order nil
     :taxonomy_family nil
     :taxonomy_genus (if (= (count name-parts) 1)
                       "[Genus Unknown]"
                       (first name-parts))
     :taxonomy_species (if (= (count name-parts) 1)
                         (first name-parts)
                         (str/join " " (rest name-parts)))}))

(defn- -m021-species-genus-migration
  [s]
  (let [spps (-get-all-species {} (state/lookup-connection s))
        build (comp #(-create-taxonomy<! % (state/lookup-connection s))
                    -m021-create-taxonomy)]
    (->> spps
         (map #(hash-map :species_id (:species_id %)
                         :taxonomy_id (int (:1 (build %)))))
         (map #(-update-sightings! % (state/lookup-connection s)))
         (doall))))

(defn- -m021-remove-unnecessary-constraints
  [s]
  (let [constraints (-get-constraints {:source_table "SIGHTING"
                                       :relation_table "SPECIES"}
                                      (state/lookup-connection s))]
    (doseq [c constraints]
      (jdbc/db-do-commands (state/lookup-connection s)
                           (str "ALTER TABLE sighting DROP CONSTRAINT "
                                (:constraintname c))))))


(let [system-config (state/system-config)
      system-state (state/config->state system-config)]
  (letfn [(migrate [state]
            (db/with-transaction [s state]
              (-m021-remove-unnecessary-constraints s)
              (-m021-species-genus-migration s)))]
    (dorun (state/map-datasets migrate system-state))))
