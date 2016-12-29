UPDATE survey
  SET survey_created_ms = 0,
      survey_updated_ms = 0
--;;
UPDATE site
  SET site_created_ms = 0,
       site_updated_ms = 0
--;;
UPDATE survey_site
  SET survey_site_created_ms = 0,
       survey_site_updated_ms = 0
--;;
UPDATE trap_station
  SET trap_station_created_ms = 0,
       trap_station_updated_ms = 0
--;;
UPDATE trap_station_session
  SET trap_station_session_created_ms = 0,
       trap_station_session_updated_ms = 0,
       trap_station_session_start_date_ms = 0,
       trap_station_session_end_date_ms = 0
--;;
UPDATE trap_station_session_camera
  SET trap_station_session_camera_created_ms = 0,
       trap_station_session_camera_updated_ms = 0
--;;
UPDATE camera
  SET camera_created_ms = 0,
       camera_updated_ms = 0
--;;
UPDATE taxonomy
  SET taxonomy_created_ms = 0,
       taxonomy_updated_ms = 0
--;;
UPDATE survey_taxonomy
  SET survey_taxonomy_created_ms = 0,
       survey_taxonomy_updated_ms = 0
--;;
UPDATE survey_file
  SET survey_file_created_ms = 0,
       survey_file_updated_ms = 0
--;;
UPDATE media
  SET media_created_ms = 0,
       media_updated_ms = 0,
       media_capture_timestamp_ms = 0
--;;
UPDATE sighting
  SET sighting_created_ms = 0,
       sighting_updated_ms = 0
--;;
UPDATE photo
  SET photo_created_ms = 0,
       photo_updated_ms = 0
