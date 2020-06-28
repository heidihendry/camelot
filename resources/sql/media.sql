-- name: create<!
INSERT INTO media (media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_detection_completed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format)
VALUES (:current_timestamp, :current_timestamp, :media_filename,
       :media_notes, :media_cameracheck, :media_attention_needed,
       :media_processed, :media_detection_completed, :media_capture_timestamp, :media_reference_quality,
       :trap_station_session_camera_id, :media_format)

-- name: get-specific
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_detection_completed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
FROM media
WHERE media_id = :media_id

-- name: get-list
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_detection_completed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
FROM media
WHERE media_id IN (:media_ids)

-- name: get-all
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_detection_completed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
FROM media
WHERE trap_station_session_camera_id = :trap_station_session_camera_id
ORDER BY media_capture_timestamp

-- name: get-most-recent-upload
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_detection_completed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
FROM media
WHERE trap_station_session_camera_id = :trap_station_session_camera_id
ORDER BY media_created DESC
FETCH FIRST 1 ROWS ONLY

-- name: get-with-ids
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_detection_completed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
FROM media
WHERE media_id IN (:media_ids)
ORDER BY media_capture_timestamp

-- name: get-all*
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_detection_completed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
FROM media

-- name: get-all-files-by-survey
SELECT (media_filename || '.' || media_format) as media_file
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
WHERE survey_id = :survey_id

-- name: get-all-files-by-survey-site
SELECT (media_filename || '.' || media_format) as media_file
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
WHERE survey_site_id = :survey_site_id

-- name: get-all-files-by-site
SELECT (media_filename || '.' || media_format) as media_file
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
WHERE site_id = :site_id

-- name: get-all-files-by-camera
SELECT (media_filename || '.' || media_format) as media_file
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
WHERE camera_id = :camera_id

-- name: get-all-files-by-trap-station
SELECT (media_filename || '.' || media_format) as media_file
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
WHERE trap_station_id = :trap_station_id

-- name: get-all-files-by-trap-station-session
SELECT (media_filename || '.' || media_format) as media_file
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
WHERE trap_station_session_id = :trap_station_session_id

-- name: get-all-files-by-trap-station-session-camera
SELECT (media_filename || '.' || media_format) as media_file
FROM media
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: update!
UPDATE media
SET media_updated = :current_timestamp,
    media_capture_timestamp = :media_capture_timestamp,
    media_notes = :media_notes,
    media_format = :media_format,
    media_cameracheck = :media_cameracheck,
    media_attention_needed = :media_attention_needed,
    media_processed = :media_processed,
    trap_station_session_camera_id = :trap_station_session_camera_id,
    media_reference_quality = :media_reference_quality,
    media_filename = :media_filename
WHERE media_id = :media_id

-- name: delete!
DELETE FROM media
WHERE media_id = :media_id

-- name: get-specific-by-filename
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_detection_completed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
FROM media
WHERE media_filename = :media_filename

-- name: update-media-flags!
UPDATE media
SET media_attention_needed = :media_attention_needed,
    media_processed = :media_processed,
    media_reference_quality = :media_reference_quality,
    media_cameracheck = :media_cameracheck
WHERE media_id = :media_id

-- name: update-processed-flag!
UPDATE media
SET media_processed = :media_processed
WHERE media_id = :media_id

-- name: update-detection-completed-flag!
UPDATE media
SET media_detection_completed = :media_detection_completed
WHERE media_id = :media_id

-- name: update-reference-quality-flag!
UPDATE media
SET media_processed = :media_reference_quality
WHERE media_id = :media_id
