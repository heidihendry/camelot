-- name: -create<!
INSERT INTO trap_station_session (trap_station_id, trap_station_session_created,
       trap_station_session_updated, trap_station_session_start_date,
       trap_station_session_end_date, trap_station_session_notes)
VALUES (:trap_station_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       :trap_station_session_start_date, :trap_station_session_end_date,
       :trap_station_session_notes)

-- name: -update!
UPDATE trap_station_session
SET trap_station_updated = CURRENT_TIMESTAMP,
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

-- name: -get-all
SELECT trap_station_session_id, trap_station_id, trap_station_session_created,
       trap_station_session_updated, trap_station_session_start_date,
       trap_station_session_end_date, trap_station_session_notes
FROM trap_station_session
WHERE trap_station_id = :trap_station_id
