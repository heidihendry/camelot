-- name: -create<!
INSERT INTO trap_station_session_camera (trap_station_session_id, trap_station_session_camera_created, trap_station_session_camera_updated, camera_id)
VALUES (:trap_station_session_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :camera_id)

-- name: -update!
UPDATE trap_station_session_camera
SET trap_station_session_camera_updated = CURRENT_TIMESTAMP,
    camera_id = :camera_id,
    trap_station_session_id = :trap_station_session_id
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: -delete!
DELETE FROM trap_station_session_camera
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: -get-specific
SELECT trap_station_session_camera_id, trap_station_session_camera_created, trap_station_session_camera_updated, camera_id, trap_station_session_id, camera_name
FROM trap_station_session_camera
LEFT JOIN camera using (camera_id)
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: -get-all
SELECT trap_station_session_camera_id, trap_station_session_camera_created, trap_station_session_camera_updated, trap_station_session_id, camera_id, camera_name
FROM trap_station_session_camera
LEFT JOIN camera using (camera_id)
WHERE trap_station_session_id = :trap_station_session_id

-- name: -get-available
SELECT camera_id, camera_name
FROM camera
WHERE camera_id NOT IN (SELECT camera_id
                        FROM trap_station_session_camera
                        WHERE trap_station_session_id = :trap_station_session_id)
