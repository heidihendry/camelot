(ns camelot.migration.create-taxonomy-up
  (:require [camelot.model.taxonomy :as taxonomy]
            [yesql.core :as sql]
            [camelot.db :as db]
            [clojure.string :as str]
            [camelot.application :as app]
            [camelot.util.config :as config]
            [camelot.model.species :as species]
            [clojure.java.jdbc :as jdbc]))

(sql/defqueries "sql/migration-helpers/021.sql" {:connection db/spec})
(sql/defqueries "sql/migration-helpers/db.sql" {:connection db/spec})

(defn create-taxonomy
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

(defn species-genus-migration
  [s]
  (let [spps (-get-all-species {} (select-keys s [:connection]))
        build (comp #(-create-taxonomy<! % (select-keys s [:connection]))
                    create-taxonomy)]
    (->> spps
         (map #(hash-map :species_id (:species_id %)
                         :taxonomy_id (int (:1 (build %)))))
         (map #(-update-sightings! % (select-keys s [:connection])))
         (doall))))

(defn remove-unnecessary-constraints
  [s]
  (let [constraints (-get-constraints {:source_table "SIGHTING"
                                       :relation_table "SPECIES"}
                                      (select-keys s [:connection]))]
    (doseq [c constraints]
      (jdbc/db-do-commands db/spec (str "ALTER TABLE sighting DROP CONSTRAINT "
                                        (:constraintname c))))))

(db/with-transaction [s (app/gen-state (config/config))]
  (remove-unnecessary-constraints s)
  (species-genus-migration s))
