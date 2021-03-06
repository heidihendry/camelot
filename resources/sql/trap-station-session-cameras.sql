-- name: create<!
INSERT INTO trap_station_session_camera (trap_station_session_id, trap_station_session_camera_created, trap_station_session_camera_updated, camera_id, trap_station_session_camera_import_path, trap_station_session_camera_media_unrecoverable)
VALUES (:trap_station_session_id, :current_timestamp, :current_timestamp, :camera_id, :trap_station_session_camera_import_path,
        :trap_station_session_camera_media_unrecoverable)

-- name: update!
UPDATE trap_station_session_camera
SET trap_station_session_camera_updated = :current_timestamp,
    camera_id = :camera_id,
    trap_station_session_id = :trap_station_session_id,
    trap_station_session_camera_import_path = :trap_station_session_camera_import_path,
    trap_station_session_camera_media_unrecoverable = :trap_station_session_camera_media_unrecoverable
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: delete!
DELETE FROM trap_station_session_camera
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: delete-media!
DELETE FROM media
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: get-specific
SELECT trap_station_session_camera_id, trap_station_session_camera_created, trap_station_session_camera_updated, camera_id, trap_station_session_id, camera_name, trap_station_session_camera_import_path, trap_station_session_camera_media_unrecoverable
FROM trap_station_session_camera
LEFT JOIN camera using (camera_id)
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: get-specific-with-camera-and-session
SELECT trap_station_session_camera_id, trap_station_session_camera_created, trap_station_session_camera_updated, camera_id, trap_station_session_id, camera_name, trap_station_session_camera_import_path, trap_station_session_camera_media_unrecoverable
FROM trap_station_session_camera
LEFT JOIN camera using (camera_id)
WHERE trap_station_session_id = :trap_station_session_id AND camera_id = :camera_id

-- name: get-specific-by-import-path
SELECT trap_station_session_camera_id, trap_station_session_camera_created, trap_station_session_camera_updated, camera_id, trap_station_session_id, camera_name, trap_station_session_camera_import_path, trap_station_session_camera_media_unrecoverable
FROM trap_station_session_camera
LEFT JOIN camera using (camera_id)
WHERE trap_station_session_camera_import_path = :trap_station_session_camera_import_path

-- name: get-all
SELECT trap_station_session_camera_id, trap_station_session_camera_created, trap_station_session_camera_updated, trap_station_session_id, camera_id, camera_name, trap_station_session_camera_import_path, trap_station_session_camera_media_unrecoverable
FROM trap_station_session_camera
LEFT JOIN camera using (camera_id)
WHERE trap_station_session_id = :trap_station_session_id

-- name: get-all*
SELECT trap_station_session_camera_id, trap_station_session_camera_created, trap_station_session_camera_updated, trap_station_session_id, camera_id, camera_name, trap_station_session_camera_import_path, trap_station_session_camera_media_unrecoverable
FROM trap_station_session_camera
LEFT JOIN camera using (camera_id)

-- name: get-available
SELECT camera_id, camera_name
FROM camera
LEFT JOIN camera_status USING (camera_status_id)
WHERE camera_status_description = 'camera-status/available'

-- name: get-alternatives
SELECT camera_id, camera_name
FROM camera
LEFT JOIN camera_status USING (camera_status_id)
WHERE camera_status_description = 'camera-status/available' OR camera_id = :camera_id

-- name: update-media-unrecoverable!
UPDATE trap_station_session_camera
SET trap_station_session_camera_media_unrecoverable = :trap_station_session_camera_media_unrecoverable,
    trap_station_session_camera_updated = :current_timestamp
WHERE trap_station_session_id = :trap_station_session_id AND camera_id = :camera_id

-- name: get-active-cameras
SELECT camera_id
FROM trap_station_session_camera
LEFT JOIN trap_station_session USING (trap_station_session_id)
WHERE trap_station_session_end_date IS NULL AND
      trap_station_session_camera_id = :trap_station_session_camera_id

-- name: get-camera-usage
SELECT camera_id, trap_station_id, trap_station_name, trap_station_session_id, trap_station_session_start_date,
       trap_station_session_end_date, survey_id
FROM trap_station_session_camera
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
WHERE camera_id = :camera_id
