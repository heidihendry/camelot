-- name: create<!
INSERT INTO species (species_created, species_updated, species_scientific_name,
       species_common_name, species_notes)
VALUES (:current_timestamp, :current_timestamp, :species_scientific_name,
       :species_common_name, :species_notes)

-- name: get-specific
SELECT species_id, species_created, species_updated, species_scientific_name,
       species_common_name, species_notes
FROM species
WHERE species_id = :species_id

-- name: get-specific-by-scientific-name
SELECT species_id, species_created, species_updated, species_scientific_name,
       species_common_name, species_notes
FROM species
WHERE LOWER(species_scientific_name) = LOWER(:species_scientific_name)

-- name: get-all
SELECT species_id, species_created, species_updated, species_scientific_name,
       species_common_name, species_notes
FROM species

-- name: update!
UPDATE species
SET species_updated = :current_timestamp,
    species_scientific_name = :species_scientific_name,
    species_common_name = :species_common_name,
    species_notes = :species_notes
WHERE species_id = :species_id

-- name: delete!
DELETE FROM species
WHERE species_id = :species_id
