(require '[camelot.db.taxonomy :as taxonomy])
(require '[yesql.core :as sql])
(require '[clojure.string :as str])
(require '[camelot.app.state :as state])
(require '[camelot.db.species :as species])
(require '[clojure.java.jdbc :as jdbc])

(sql/defqueries "sql/migration-helpers/021.sql" {:connection state/spec})
(sql/defqueries "sql/migration-helpers/db.sql" {:connection state/spec})

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
  (let [spps (-get-all-species {} (select-keys (:database s) [:connection]))
        build (comp #(-create-taxonomy<! % (select-keys (:database s) [:connection]))
                    -m021-create-taxonomy)]
    (->> spps
         (map #(hash-map :species_id (:species_id %)
                         :taxonomy_id (int (:1 (build %)))))
         (map #(-update-sightings! % (select-keys (:database s) [:connection])))
         (doall))))

(defn- -m021-remove-unnecessary-constraints
  [s]
  (let [constraints (-get-constraints {:source_table "SIGHTING"
                                       :relation_table "SPECIES"}
                                      (select-keys (:database s) [:connection]))]
    (doseq [c constraints]
      (jdbc/db-do-commands state/spec (str "ALTER TABLE sighting DROP CONSTRAINT "
                                           (:constraintname c))))))

(db/with-transaction [s (state/gen-state*)]
  (-m021-remove-unnecessary-constraints s)
  (-m021-species-genus-migration s))
