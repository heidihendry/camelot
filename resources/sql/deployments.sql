-- name: -get-specific
SELECT trap_station_session_id, trap_station_session_created,
       trap_station_session_updated, trap_station_id, trap_station_name, site_id,
       survey_site_id, trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes, survey_site_id, site_name,
       trap_station_session_start_date, trap_station_session_end_date, camera_id,
       camera_name, camera_status_id
FROM trap_station_session
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_id)
LEFT JOIN survey_site USING (survey_site_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN site USING (site_id)
LEFT JOIN camera_status USING (camera_status_id)
WHERE trap_station_session_id = :trap_station_session_id

-- name: -get-all
SELECT trap_station_session_id, trap_station_session_created,
       trap_station_session_updated, trap_station_id, trap_station_name, site_id,
       survey_site_id, trap_station_name, trap_station_longitude, trap_station_latitude,
       trap_station_altitude, trap_station_notes, survey_site_id, site_name,
       trap_station_session_start_date, trap_station_session_end_date, camera_id,
       camera_name, camera_status_id
FROM trap_station_session
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_id)
LEFT JOIN survey_site USING (survey_site_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN site USING (site_id)
LEFT JOIN camera_status USING (camera_status_id)
WHERE survey_id = :survey_id AND trap_station_session_end_date IS NULL

-- name: -set-session-end-date!
UPDATE trap_station_session
       SET trap_station_session_end_date = :trap_station_session_end_date,
           trap_station_session_updated = CURRENT_TIMESTAMP
WHERE trap_station_session_id = :trap_station_session_id
      AND trap_station_session_end_date IS NULL

-- name: -set-camera-status!
UPDATE camera
       SET camera_status_id = :camera_status_id,
           camera_updated = CURRENT_TIMESTAMP
WHERE camera_id = :camera_id
