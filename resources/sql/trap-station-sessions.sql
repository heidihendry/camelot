-- name: -create<!
INSERT INTO trap_station_session (trap_station_id, trap_station_session_created,
       trap_station_session_updated, trap_station_session_start_date,
       trap_station_session_end_date, trap_station_session_notes)
VALUES (:trap_station_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       :trap_station_session_start_date, :trap_station_session_end_date,
       :trap_station_session_notes)

-- name: -update!
UPDATE trap_station_session
SET trap_station_session_updated = CURRENT_TIMESTAMP,
    trap_station_id = :trap_station_id,
    trap_station_session_start_date = :trap_station_session_start_date,
    trap_station_session_end_date = :trap_station_session_end_date,
    trap_station_session_notes = :trap_station_session_notes
WHERE trap_station_session_id = :trap_station_session_id

-- name: -delete!
DELETE FROM trap_station_session
WHERE trap_station_session_id = :trap_station_session_id

-- name: -get-specific
SELECT trap_station_session_id, trap_station_id, trap_station_session_created,
       trap_station_session_updated, trap_station_session_start_date,
       trap_station_session_end_date, trap_station_session_notes
FROM trap_station_session
WHERE trap_station_session_id = :trap_station_session_id

-- name: -get-specific-by-dates
SELECT trap_station_session_id, trap_station_id, trap_station_session_created,
       trap_station_session_updated, trap_station_session_start_date,
       trap_station_session_end_date, trap_station_session_notes
FROM trap_station_session
WHERE trap_station_id = :trap_station_id
      AND trap_station_session_start_date = :trap_station_session_start_date
      AND trap_station_session_end_date = :trap_station_session_end_date

-- name: -get-all
SELECT trap_station_session_id, trap_station_id, trap_station_session_created,
       trap_station_session_updated, trap_station_session_start_date,
       trap_station_session_end_date, trap_station_session_notes
FROM trap_station_session
WHERE trap_station_id = :trap_station_id

-- name: -get-active
SELECT camera_id, camera_name, trap_station_session_camera_id
FROM camera
LEFT JOIN trap_station_session_camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
WHERE (trap_station_session_start_date >= :trap_station_session_start_date AND
       trap_station_session_start_date < :trap_station_session_end_date)
       OR
      (trap_station_session_end_date <= :trap_station_session_end_date AND
       trap_station_session_end_date > :trap_station_session_start_date)
       OR
      (trap_station_session_start_date <= :trap_station_session_start_date AND
       trap_station_session_end_date >= :trap_station_session_end_date)

-- name: -get-specific-by-trap-station-session-camera-id
SELECT trap_station_session_id, trap_station_id, trap_station_session_created,
       trap_station_session_updated, trap_station_session_start_date,
       trap_station_session_end_date, trap_station_session_notes
FROM trap_station_session
LEFT JOIN trap_station_session_camera USING (trap_station_session_id)
WHERE trap_station_session_camera_id = :trap_station_session_camera_id

-- name: -set-session-end-date!
UPDATE trap_station_session
       SET trap_station_session_end_date = :trap_station_session_end_date,
           trap_station_session_updated = CURRENT_TIMESTAMP
WHERE trap_station_session_id = :trap_station_session_id
      AND trap_station_session_end_date IS NULL
