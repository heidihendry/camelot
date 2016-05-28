-- name: -create<!
INSERT INTO trap_station (survey_site_id, trap_station_created, trap_station_updated,
                          trap_station_name, trap_station_longitude, trap_station_latitude,
                          trap_station_altitude, trap_station_notes)
VALUES (:survey_site_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :trap_station_name,
       :trap_station_longitude, :trap_station_latitude, :trap_station_altitude,
       :trap_station_notes)

-- name: -update!
UPDATE trap_station
SET trap_station_updated = CURRENT_TIMESTAMP,
    survey_site_id = :survey_site_id,
    trap_station_name = :trap_station_name,
    trap_station_longitude = :trap_station_longitude,
    trap_station_latitude = :trap_station_latitude,
    trap_station_altitude = :trap_station_altitude,
    trap_station_notes = :trap_station_notes
WHERE trap_station_id = :trap_station_id

-- name: -delete!
DELETE FROM trap_station
WHERE trap_station_id = :trap_station_id

-- name: -get-specific
SELECT trap_station_id, survey_site_id, trap_station_created, trap_station_updated,
       trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes
FROM trap_station
WHERE trap_station_id = :trap_station_id

-- name: -get-specific-by-location
SELECT trap_station_id, survey_site_id, trap_station_created, trap_station_updated,
       trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes
FROM trap_station
WHERE survey_site_id = :survey_site_id
      AND trap_station_longitude = :trap_station_longitude
      AND trap_station_latitude = :trap_station_latitude

-- name: -get-all
SELECT trap_station_id, survey_site_id, trap_station_created, trap_station_updated,
       trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes
FROM trap_station
WHERE survey_site_id = :survey_site_id
