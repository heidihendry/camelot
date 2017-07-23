-- name: -create<!
INSERT INTO survey (survey_name, survey_created, survey_updated, survey_directory,
       survey_sampling_point_density, survey_sighting_independence_threshold, survey_notes,
       survey_bulk_import_mode)
VALUES (:survey_name, :current_timestamp, :current_timestamp, :survey_directory,
       :survey_sampling_point_density, :survey_sighting_independence_threshold, :survey_notes,
       :survey_bulk_import_mode)

-- name: -update!
UPDATE survey
SET survey_updated = :current_timestamp,
    survey_name = :survey_name,
    survey_sighting_independence_threshold = :survey_sighting_independence_threshold,
    survey_sampling_point_density = :survey_sampling_point_density,
    survey_directory = :survey_directory,
    survey_notes = :survey_notes
WHERE survey_id = :survey_id

-- name: -set-bulk-import-mode!
UPDATE survey
SET survey_bulk_import_mode = :survey_bulk_import_mode,
    survey_updated = :current_timestamp
WHERE survey_id = :survey_id

-- name: -delete!
DELETE FROM survey
WHERE survey_id = :survey_id

-- name: -get-specific-by-name
SELECT survey_id, survey_created, survey_updated, survey_name, survey_directory,
       survey_sampling_point_density, survey_sighting_independence_threshold, survey_notes,
       survey_bulk_import_mode
FROM survey
WHERE survey_name = :survey_name

-- name: -get-specific
SELECT survey_id, survey_created, survey_updated, survey_name, survey_directory,
       survey_sampling_point_density, survey_sighting_independence_threshold, survey_notes,
       survey_bulk_import_mode
FROM survey
WHERE survey_id = :survey_id

-- name: -get-all
SELECT survey_id, survey_created, survey_updated, survey_name, survey_directory,
       survey_sampling_point_density, survey_sighting_independence_threshold, survey_notes,
       survey_bulk_import_mode
FROM survey

-- name: -get-active-cameras
SELECT camera_id
FROM survey
LEFT JOIN survey_site USING (survey_id)
LEFT JOIN trap_station USING (survey_site_id)
LEFT JOIN trap_station_session USING (trap_station_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_id)
WHERE trap_station_session_end_date IS NULL AND
      survey_id = :survey_id
