--name: -get-all-species
SELECT species_id, species_notes, species_common_name, species_created, species_updated,
       species_scientific_name
FROM species

--name: -create-taxonomy<!
INSERT INTO taxonomy (taxonomy_created, taxonomy_updated, taxonomy_class, taxonomy_order,
       taxonomy_family, taxonomy_genus, taxonomy_species, taxonomy_common_name,
       taxonomy_notes)
VALUES (:taxonomy_created, :taxonomy_updated, :taxonomy_class, :taxonomy_order,
       :taxonomy_family, :taxonomy_genus, :taxonomy_species, :taxonomy_common_name,
       :taxonomy_notes)

-- name: -update-sightings!
UPDATE sighting
  SET taxonomy_id = :taxonomy_id
  WHERE species_id = :species_id
