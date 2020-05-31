(require '[yesql.core :as sql])
(require '[clojure.string :as str])
(require '[camelot.state.datasets :as datasets])
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
  (let [conn {:connection (datasets/lookup-connection (:datasets s))}
        spps (-get-all-species {} conn)
        build (comp #(-create-taxonomy<! % conn)
                    -m021-create-taxonomy)]
    (->> spps
         (map #(hash-map :species_id (:species_id %)
                         :taxonomy_id (int (:1 (build %)))))
         (map #(-update-sightings! % conn))
         (doall))))

(defn- -m021-remove-unnecessary-constraints
  [s]
  (let [conn {:connection (datasets/lookup-connection (:datasets s))}
        constraints (-get-constraints {:source_table "SIGHTING"
                                       :relation_table "SPECIES"}
                                      conn)]
    (doseq [c constraints]
      (jdbc/db-do-commands (:connection conn)
                           (str "ALTER TABLE sighting DROP CONSTRAINT "
                                (:constraintname c))))))

(db/with-transaction [s camelot.system.db.core/*migration-state*]
  (-m021-remove-unnecessary-constraints s)
  (-m021-species-genus-migration s))
