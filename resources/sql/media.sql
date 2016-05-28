-- name: -create<!
INSERT INTO media (media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_capture_timestamp, trap_station_session_camera_id)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :media_filename,
       :media_notes, :media_cameracheck, :media_attention_needed,
       :media_capture_timestamp, :trap_station_session_camera_id)

-- name: -get-specific
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_capture_timestamp, trap_station_session_camera_id
FROM media
WHERE media_id = :media_id

-- name: -get-all
SELECT media_id, media_created, media_updated, media_filename,
       media_notes, media_cameracheck, media_attention_needed,
       media_capture_timestamp, trap_station_session_camera_id
FROM media
