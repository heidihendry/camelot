-- name: -create<!
INSERT INTO media (media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :media_filename,
       :media_notes, :media_cameracheck, :media_attention_needed,
       :media_processed, :media_capture_timestamp, :media_reference_quality,
       :trap_station_session_camera_id, :media_format)

-- name: -get-specific
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
FROM media
WHERE media_id = :media_id

-- name: -get-all
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
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
    media_processed = :media_processed,
    trap_station_session_camera_id = :trap_station_session_camera_id,
    media_reference_quality = :media_reference_quality,
    media_filename = :media_filename
WHERE media_id = :media_id

-- name: -delete!
DELETE FROM media
WHERE media_id = :media_id

-- name: -get-specific-by-filename
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_processed, media_capture_timestamp, media_reference_quality,
       trap_station_session_camera_id, media_format
FROM media
WHERE media_filename = :media_filename

-- name: -update-media-flags!
UPDATE media
SET media_attention_needed = :media_attention_needed,
    media_processed = :media_processed,
    media_reference_quality = :media_reference_quality
WHERE media_id = :media_id

-- name: -update-processed-flag!
UPDATE media
SET media_processed = :media_processed
WHERE media_id = :media_id

-- name: -update-reference-quality-flag!
UPDATE media
SET media_processed = :media_reference_quality
WHERE media_id = :media_id
