ALTER TABLE survey_site ADD CONSTRAINT SS_UNIQUE_JOIN UNIQUE (survey_id, site_id)
--;;
ALTER TABLE trap_station_session_camera ADD CONSTRAINT TSSC_UNIQUE_JOIN UNIQUE (camera_id, trap_station_session_id)
