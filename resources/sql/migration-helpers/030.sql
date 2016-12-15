-- name: -get-all-survey
SELECT survey_id, survey_created, survey_updated, survey_name, survey_directory,
       survey_sampling_point_density, survey_sighting_independence_threshold, survey_notes
FROM survey

-- name: -get-all-taxonomy
SELECT taxonomy_id, taxonomy_created, taxonomy_updated, taxonomy_class, taxonomy_order,
       taxonomy_family, taxonomy_genus, taxonomy_species, taxonomy_common_name,
       species_mass_id, taxonomy_notes
FROM taxonomy

-- name: -delete!
DELETE FROM survey_taxonomy
WHERE survey_taxonomy_id = :survey_taxonomy_id

-- name: -get-all-survey-taxonomy
SELECT survey_id, taxonomy_id, survey_taxonomy_created,
       survey_taxonomy_updated, survey_taxonomy_id
FROM survey_taxonomy

-- name: -create<!
INSERT INTO survey_taxonomy (survey_taxonomy_created,
       survey_taxonomy_updated, survey_id, taxonomy_id)
       VALUES
       (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :survey_id, :taxonomy_id)
