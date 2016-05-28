-- name: -create<!
INSERT INTO species (species_created, species_updated, species_scientific_name,
       species_common_name, species_notes)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :species_scientific_name,
       :species_common_name, :species_notes)

-- name: -get-specific
SELECT species_id, species_created, species_updated, species_scientific_name,
       species_common_name, species_notes
FROM species
WHERE species_id = :species_id

-- name: -get-specific-by-scientific-name
SELECT species_id, species_created, species_updated, species_scientific_name,
       species_common_name, species_notes
FROM species
WHERE species_scientific_name = :species_scientific_name

-- name: -get-all
SELECT species_id, species_created, species_updated, species_scientific_name,
       species_common_name, species_notes
FROM species
