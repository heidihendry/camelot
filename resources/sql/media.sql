-- name: -create<!
INSERT INTO media (media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_capture_timestamp, trap_station_session_camera_id,
       media_format)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :media_filename,
       :media_notes, :media_cameracheck, :media_attention_needed,
       :media_capture_timestamp, :trap_station_session_camera_id,
       :media_format)

-- name: -get-specific
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_capture_timestamp, trap_station_session_camera_id,
       media_format
FROM media
WHERE media_id = :media_id

-- name: -get-all
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_capture_timestamp, trap_station_session_camera_id,
       media_format
FROM media
WHERE trap_station_session_camera_id = :trap_station_session_camera_id
ORDER BY media_capture_timestamp

-- name: -update!
UPDATE media
SET media_updated = CURRENT_TIMESTAMP,
    media_capture_timestamp = :media_capture_timestamp,
    media_notes = :media_notes,
    media_format = :media_format,
    media_cameracheck = :media_cameracheck,
    media_attention_needed = :media_attention_needed,
    trap_station_session_camera_id = :trap_station_session_camera_id
    media_filename = :media_filename
WHERE media_id = :media_id

-- name: -delete!
DELETE FROM media
WHERE media_id = :media_id

-- name: -get-specific-by-filename
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_capture_timestamp, trap_station_session_camera_id,
       media_format
FROM media
WHERE media_filename = :media_filename
