-- name: -all-media
SELECT media_id, media_created, media_updated, media_filename, media_format, media_cameracheck, media_attention_needed, media_processed,
       media_reference_quality, media_capture_timestamp, trap_station_session_camera_id
FROM media
LEFT JOIN photo USING (media_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
LEFT JOIN site USING (site_id)
LEFT JOIN survey USING (survey_id)
ORDER BY trap_station_id, camera_id, trap_station_session_start_date, trap_station_session_id, media_capture_timestamp

-- name: -all-media-for-survey
SELECT media_id, media_created, media_updated, media_filename, media_format, media_cameracheck, media_attention_needed, media_processed,
       media_reference_quality, media_capture_timestamp, trap_station_session_camera_id
FROM media
LEFT JOIN photo USING (media_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
LEFT JOIN site USING (site_id)
LEFT JOIN survey USING (survey_id)
WHERE survey_id = :survey_id
ORDER BY trap_station_id, camera_id, trap_station_session_start_date, trap_station_session_id, media_capture_timestamp

-- name: -hierarchy-data
SELECT trap_station_session_id, trap_station_id, trap_station_name,
       trap_station_longitude, trap_station_latitude, site_sublocation,
       site_city, camera_id, camera_name, camera_make, camera_model,
       survey_site_id, survey_id, site_id, site_name,
       survey_name, site_country, site_state_province, trap_station_session_camera_id
FROM trap_station_session_camera
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
LEFT JOIN site USING (site_id)
LEFT JOIN survey USING (survey_id)
