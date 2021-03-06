-- name: create<!
INSERT INTO taxonomy (taxonomy_created, taxonomy_updated, taxonomy_class, taxonomy_order,
       taxonomy_family, taxonomy_genus, taxonomy_species, taxonomy_common_name,
       species_mass_id, taxonomy_notes)
VALUES (:current_timestamp, :current_timestamp, :taxonomy_class, :taxonomy_order,
       :taxonomy_family, :taxonomy_genus, :taxonomy_species, :taxonomy_common_name,
       :species_mass_id, :taxonomy_notes)

-- name: clone<!
INSERT INTO taxonomy (taxonomy_id, taxonomy_created, taxonomy_updated, taxonomy_class, taxonomy_order,
       taxonomy_family, taxonomy_genus, taxonomy_species, taxonomy_common_name,
       species_mass_id, taxonomy_notes)
VALUES (:taxonomy_id, :taxonomy_created, :taxonomy_updated, :taxonomy_class, :taxonomy_order,
       :taxonomy_family, :taxonomy_genus, :taxonomy_species, :taxonomy_common_name,
       :taxonomy_notes)

-- name: get-specific
SELECT taxonomy_id, taxonomy_created, taxonomy_updated, taxonomy_class, taxonomy_order,
       taxonomy_family, taxonomy_genus, taxonomy_species, taxonomy_common_name,
       species_mass_id, taxonomy_notes
FROM taxonomy
WHERE taxonomy_id = :taxonomy_id

-- name: get-specific-by-taxonomy
SELECT taxonomy_id, taxonomy_created, taxonomy_updated, taxonomy_class, taxonomy_order,
       taxonomy_family, taxonomy_genus, taxonomy_species, taxonomy_common_name,
       species_mass_id, taxonomy_notes
FROM taxonomy
WHERE LOWER(taxonomy_species) = LOWER(:taxonomy_species) AND
      LOWER(taxonomy_genus) = LOWER(:taxonomy_genus)

-- name: get-all
SELECT taxonomy_id, taxonomy_created, taxonomy_updated, taxonomy_class, taxonomy_order,
       taxonomy_family, taxonomy_genus, taxonomy_species, taxonomy_common_name,
       species_mass_id, taxonomy_notes
FROM taxonomy

-- name: get-all-for-survey
SELECT taxonomy_id, taxonomy_created, taxonomy_updated, taxonomy_class, taxonomy_order,
       taxonomy_family, taxonomy_genus, taxonomy_species, taxonomy_common_name,
       species_mass_id, taxonomy_notes
FROM taxonomy
LEFT JOIN survey_taxonomy USING (taxonomy_id)
WHERE survey_id = :survey_id

-- name: update!
UPDATE taxonomy
SET taxonomy_updated = :current_timestamp,
    taxonomy_class = :taxonomy_class,
    taxonomy_order = :taxonomy_order,
    taxonomy_family = :taxonomy_family,
    taxonomy_genus = :taxonomy_genus,
    taxonomy_species = :taxonomy_species,
    taxonomy_common_name = :taxonomy_common_name,
    species_mass_id = :species_mass_id,
    taxonomy_notes = :taxonomy_notes
WHERE taxonomy_id = :taxonomy_id

-- name: delete!
DELETE FROM taxonomy
WHERE taxonomy_id = :taxonomy_id

-- name: delete-from-survey!
DELETE FROM survey_taxonomy
WHERE taxonomy_id = :taxonomy_id AND survey_id = :survey_id
