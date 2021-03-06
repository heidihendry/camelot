-- name: create<!
INSERT INTO trap_station (survey_site_id, trap_station_created, trap_station_updated,
                          trap_station_name, trap_station_longitude, trap_station_latitude,
                          trap_station_altitude, trap_station_notes,
                          trap_station_distance_above_ground,
                          trap_station_distance_to_road,
                          trap_station_distance_to_river,
                          trap_station_distance_to_settlement)
VALUES (:survey_site_id, :current_timestamp, :current_timestamp, :trap_station_name,
       :trap_station_longitude, :trap_station_latitude, :trap_station_altitude,
       :trap_station_notes, :trap_station_distance_above_ground,
       :trap_station_distance_to_road, :trap_station_distance_to_river,
       :trap_station_distance_to_settlement)

-- name: update!
UPDATE trap_station
SET trap_station_updated = :current_timestamp,
    survey_site_id = :survey_site_id,
    trap_station_name = :trap_station_name,
    trap_station_longitude = :trap_station_longitude,
    trap_station_latitude = :trap_station_latitude,
    trap_station_altitude = :trap_station_altitude,
    trap_station_notes = :trap_station_notes,
    trap_station_distance_above_ground = :trap_station_distance_above_ground,
    trap_station_distance_to_road = :trap_station_distance_to_road,
    trap_station_distance_to_river = :trap_station_distance_to_river,
    trap_station_distance_to_settlement = :trap_station_distance_to_settlement
WHERE trap_station_id = :trap_station_id

-- name: delete!
DELETE FROM trap_station
WHERE trap_station_id = :trap_station_id

-- name: get-specific
SELECT trap_station_id, survey_site_id, trap_station_created, trap_station_updated,
       trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes, trap_station_distance_above_ground,
       trap_station_distance_to_road, trap_station_distance_to_river,
       trap_station_distance_to_settlement
FROM trap_station
WHERE trap_station_id = :trap_station_id

-- name: get-specific-by-name-and-location
SELECT trap_station_id, survey_site_id, trap_station_created, trap_station_updated,
       trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes, trap_station_distance_above_ground,
       trap_station_distance_to_road, trap_station_distance_to_river,
       trap_station_distance_to_settlement
FROM trap_station
WHERE survey_site_id = :survey_site_id
      AND trap_station_name = :trap_station_name
      AND trap_station_longitude = :trap_station_longitude
      AND trap_station_latitude = :trap_station_latitude

-- name: get-all
SELECT trap_station_id, survey_site_id, trap_station_created, trap_station_updated,
       trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes, trap_station_distance_above_ground,
       trap_station_distance_to_road, trap_station_distance_to_river,
       trap_station_distance_to_settlement
FROM trap_station
WHERE survey_site_id = :survey_site_id

-- name: get-all*
SELECT trap_station_id, survey_site_id, trap_station_created, trap_station_updated,
       trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes, trap_station_distance_above_ground,
       trap_station_distance_to_road, trap_station_distance_to_river,
       trap_station_distance_to_settlement
FROM trap_station

-- name: get-all-for-survey
SELECT trap_station_id, survey_site_id, trap_station_created, trap_station_updated,
       trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes, trap_station_distance_above_ground,
       trap_station_distance_to_road, trap_station_distance_to_river,
       trap_station_distance_to_settlement
FROM trap_station
LEFT JOIN survey_site USING (survey_site_id)
WHERE survey_id = :survey_id

-- name: get-active-cameras
SELECT camera_id
FROM trap_station
LEFT JOIN trap_station_session USING (trap_station_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_id)
WHERE trap_station_session_end_date IS NULL AND
      trap_station_id = :trap_station_id
