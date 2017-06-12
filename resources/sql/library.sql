-- name: -all-media
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
ORDER BY trap_station_id, camera_id, trap_station_session_start_date, trap_station_session_id, media_capture_timestamp

-- name: -all-media-with-survey-id
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
WHERE survey_id = :field_value
ORDER BY trap_station_id, camera_id, trap_station_session_start_date, trap_station_session_id, media_capture_timestamp

-- name: -all-media-with-site-name
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
LEFT JOIN site USING (site_id)
WHERE LOWER(site_name) = LOWER(:field_value)

-- name: -all-media-with-camera-name
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
WHERE LOWER(camera_name) = LOWER(:field_value)

-- name: -all-media-with-trap-station-id
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
WHERE trap_station_id = :field_value

-- name: -all-media-with-reference-quality-sighting
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
WHERE media_reference_quality = :field_value

-- name: -all-media-with-attention-needed-flag
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
WHERE media_attention_needed = :field_value

-- name: -all-media-with-media-processed-flag
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
WHERE media_processed = :field_value

-- name: -all-media-with-media-cameracheck-flag
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
WHERE media_cameracheck = :field_value

-- name: -all-media-with-taxonomy-species
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN sighting USING (media_id)
LEFT JOIN taxonomy USING (taxonomy_id)
WHERE LOWER(taxonomy_species) = LOWER(:field_value)

-- name: -all-media-with-taxonomy-genus
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN sighting USING (media_id)
LEFT JOIN taxonomy USING (taxonomy_id)
WHERE LOWER(taxonomy_genus) = LOWER(:field_value)

-- name: -all-media-with-taxonomy-common-name
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN sighting USING (media_id)
LEFT JOIN taxonomy USING (taxonomy_id)
WHERE LOWER(taxonomy_common_name) = LOWER(:field_value)

-- name: -all-media-with-taxonomy-scientific-name
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN sighting USING (media_id)
LEFT JOIN taxonomy USING (taxonomy_id)
WHERE LOWER(taxonomy_genus || ' ' || taxonomy_species) = LOWER(:field_value)

-- name: -all-media-with-taxonomy-id
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN sighting USING (media_id)
LEFT JOIN taxonomy USING (taxonomy_id)
WHERE taxonomy_id = :field_value

-- name: -all-media-ids
SELECT media_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
ORDER BY trap_station_id, camera_id, trap_station_session_start_date, trap_station_session_id, media_capture_timestamp

-- name: -all-media-ids-for-survey
SELECT media_id, camera_id
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
WHERE survey_id = :field_value
ORDER BY trap_station_id, camera_id, trap_station_session_start_date, trap_station_session_id, media_capture_timestamp

-- name: -hierarchy-data
SELECT trap_station_session_id, trap_station_id, trap_station_name,
       trap_station_longitude, trap_station_latitude, site_sublocation,
       site_city, camera_id, camera_name, camera_make, camera_model,
       survey_site_id, survey_id, site_id, site_name,
       survey_name, site_country, site_state_province, trap_station_session_camera_id
FROM trap_station_session_camera
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
LEFT JOIN site USING (site_id)
LEFT JOIN survey USING (survey_id)
