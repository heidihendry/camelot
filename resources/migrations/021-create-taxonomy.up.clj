(require '[clojure.java.jdbc :as jdbc])
(require '[yesql.core :as sql])
(require '[clojure.string :as str])

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
  [conn]
  (let [conn {:connection conn}
        spps (-get-all-species {} conn)
        build (comp #(-create-taxonomy<! % conn)
                    -m021-create-taxonomy)]
    (->> spps
         (map #(hash-map :species_id (:species_id %)
                         :taxonomy_id (int (:1 (build %)))))
         (map #(-update-sightings! % conn))
         (doall))))

(defn- -m021-remove-unnecessary-constraints
  [conn]
  (let [conn {:connection conn}
        constraints (-get-constraints {:source_table "SIGHTING"
                                       :relation_table "SPECIES"}
                                      conn)]
    (doseq [c constraints]
      (jdbc/db-do-commands (:connection conn)
                           (str "ALTER TABLE sighting DROP CONSTRAINT "
                                (:constraintname c))))))

(jdbc/with-db-transaction [conn camelot.migration/*connection*]
  (-m021-remove-unnecessary-constraints conn)
  (-m021-species-genus-migration conn))
