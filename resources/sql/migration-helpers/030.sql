-- name: -get-all-survey
SELECT survey_id, survey_created, survey_updated, survey_name, survey_directory,
       survey_sampling_point_density, survey_sighting_independence_threshold, survey_notes
FROM survey

-- name: -get-all-taxonomy
SELECT taxonomy_id, taxonomy_created, taxonomy_updated, taxonomy_class, taxonomy_order,
       taxonomy_family, taxonomy_genus, taxonomy_species, taxonomy_common_name,
       species_mass_id, taxonomy_notes
FROM taxonomy
