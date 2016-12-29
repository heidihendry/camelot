-- name: -get-surveys
SELECT survey_id, survey_created, survey_updated
FROM survey

-- name: -migrate-survey!
UPDATE survey
  SET survey_created_ms = :survey_created_ms,
      survey_updated_ms = :survey_updated_ms
WHERE survey_id = :survey_id

-- name: -get-sites
SELECT site_id, site_created, site_updated
FROM site

-- name: -migrate-site!
UPDATE site
  SET site_created_ms = :site_created_ms,
       site_updated_ms = :site_updated_ms
WHERE site_id = :site_id

-- name: -get-survey-sites
SELECT survey_site_id, survey_site_created, survey_site_updated
FROM survey_site

-- name: -migrate-survey-site!
UPDATE survey_site
  SET survey_site_created_ms = :survey_site_created_ms,
       survey_site_updated_ms = :survey_site_updated_ms
WHERE survey_site_id = :survey_site_id

-- name: -get-trap-stations
SELECT trap_station_id, trap_station_created, trap_station_updated
FROM trap_station

-- name: -migrate-trap-station!
UPDATE trap_station
  SET trap_station_created_ms = :trap_station_created_ms,
       trap_station_updated_ms = :trap_station_updated_ms
WHERE trap_station_id = :trap_station_id

-- name: -get-trap-station-sessions
SELECT trap_station_session_id, trap_station_session_created, trap_station_session_updated,
       trap_station_session_start_date, trap_station_session_end_date
FROM trap_station_session

-- name: -migrate-trap-station-session!
UPDATE trap_station_session
  SET trap_station_session_created_ms = :trap_station_session_created_ms,
       trap_station_session_updated_ms = :trap_station_session_updated_ms,
       trap_station_session_start_date_ms = :trap_station_session_start_date_ms,
       trap_station_session_end_date_ms = :trap_station_session_end_date_ms
WHERE trap_station_session_id = :trap_station_session_id

-- name: -get-trap-station-session-cameras
SELECT trap_station_session_camera_id, trap_station_session_camera_created, trap_station_session_camera_updated
FROM trap_station_session_camera

-- name: -migrate-trap-station-session-camera!
UPDATE trap_station_session_camera
  SET trap_station_session_camera_created_ms = :trap_station_session_camera_created_ms,
       trap_station_session_camera_updated_ms = :trap_station_session_camera_updated_ms
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: -get-cameras
SELECT camera_id, camera_created, camera_updated
FROM camera

-- name: -migrate-camera!
UPDATE camera
  SET camera_created_ms = :camera_created_ms,
       camera_updated_ms = :camera_updated_ms
WHERE camera_id = :camera_id

-- name: -get-taxonomies
SELECT taxonomy_id, taxonomy_created, taxonomy_updated
FROM taxonomy

-- name: -migrate-taxonomy!
UPDATE taxonomy
  SET taxonomy_created_ms = :taxonomy_created_ms,
       taxonomy_updated_ms = :taxonomy_updated_ms
WHERE taxonomy_id = :taxonomy_id

-- name: -get-survey-taxonomies
SELECT survey_taxonomy_id, survey_taxonomy_created, survey_taxonomy_updated
FROM survey_taxonomy

-- name: -migrate-survey-taxonomy!
UPDATE survey_taxonomy
  SET survey_taxonomy_created_ms = :survey_taxonomy_created_ms,
       survey_taxonomy_updated_ms = :survey_taxonomy_updated_ms
WHERE survey_taxonomy_id = :survey_taxonomy_id

-- name: -get-survey-files
SELECT survey_file_id, survey_file_created, survey_file_updated
FROM survey_file

-- name: -migrate-survey-file!
UPDATE survey_file
  SET survey_file_created_ms = :survey_file_created_ms,
       survey_file_updated_ms = :survey_file_updated_ms
WHERE survey_file_id = :survey_file_id

-- name: -get-media
SELECT media_id, media_created, media_updated, media_capture_timestamp
FROM media

-- name: -migrate-media!
UPDATE media
  SET media_created_ms = :media_created_ms,
       media_updated_ms = :media_updated_ms,
       media_capture_timestamp_ms = :media_capture_timestamp_ms
WHERE media_id = :media_id

-- name: -get-sightings
SELECT sighting_id, sighting_created, sighting_updated
FROM sighting

-- name: -migrate-sighting!
UPDATE sighting
  SET sighting_created_ms = :sighting_created_ms,
       sighting_updated_ms = :sighting_updated_ms
WHERE sighting_id = :sighting_id

-- name: -get-photos
SELECT photo_id, photo_created, photo_updated
FROM photo

-- name: -migrate-photo!
UPDATE photo
  SET photo_created_ms = :photo_created_ms,
       photo_updated_ms = :photo_updated_ms
WHERE photo_id = :photo_id
