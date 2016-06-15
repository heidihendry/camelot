-- name: -all-media
SELECT media_id, media_created, media_updated, media_filename, media_format, media_cameracheck, media_attention_needed,
       media_capture_timestamp, trap_station_session_camera_id, trap_station_session_id, trap_station_id,
       trap_station_name, trap_station_longitude, trap_station_latitude, site_sublocation, site_city, camera_id,
       camera_name, camera_make, camera_model, survey_site_id, survey_id, site_id, site_name
FROM media
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN camera USING (camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
LEFT JOIN site USING (site_id)
ORDER BY trap_station_session_id, media_capture_timestamp
