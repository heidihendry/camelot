ALTER TABLE survey DROP COLUMN survey_created
--;;
ALTER TABLE survey ADD COLUMN survey_created TIMESTAMP
--;;
ALTER TABLE survey DROP COLUMN survey_updated
--;;
ALTER TABLE survey ADD COLUMN survey_updated TIMESTAMP
--;;
ALTER TABLE site DROP COLUMN site_created
--;;
ALTER TABLE site ADD COLUMN site_created TIMESTAMP
--;;
ALTER TABLE site DROP COLUMN site_updated
--;;
ALTER TABLE site ADD COLUMN site_updated TIMESTAMP
--;;
ALTER TABLE survey_site DROP COLUMN survey_site_created
--;;
ALTER TABLE survey_site ADD COLUMN survey_site_created TIMESTAMP
--;;
ALTER TABLE survey_site DROP COLUMN survey_site_updated
--;;
ALTER TABLE survey_site ADD COLUMN survey_site_updated TIMESTAMP
--;;
ALTER TABLE camera DROP COLUMN camera_created
--;;
ALTER TABLE camera ADD COLUMN camera_created TIMESTAMP
--;;
ALTER TABLE camera DROP COLUMN camera_updated
--;;
ALTER TABLE camera ADD COLUMN camera_updated TIMESTAMP
--;;
ALTER TABLE trap_station DROP COLUMN trap_station_created
--;;
ALTER TABLE trap_station ADD COLUMN trap_station_created TIMESTAMP
--;;
ALTER TABLE trap_station DROP COLUMN trap_station_updated
--;;
ALTER TABLE trap_station ADD COLUMN trap_station_updated TIMESTAMP
--;;
ALTER TABLE trap_station_session DROP COLUMN trap_station_session_created
--;;
ALTER TABLE trap_station_session ADD COLUMN trap_station_session_created TIMESTAMP
--;;
ALTER TABLE trap_station_session DROP COLUMN trap_station_session_updated
--;;
ALTER TABLE trap_station_session ADD COLUMN trap_station_session_updated TIMESTAMP
--;;
ALTER TABLE trap_station_session DROP COLUMN trap_station_session_start_date
--;;
ALTER TABLE trap_station_session ADD COLUMN trap_station_session_start_date TIMESTAMP
--;;
ALTER TABLE trap_station_session DROP COLUMN trap_station_session_end_date
--;;
ALTER TABLE trap_station_session ADD COLUMN trap_station_session_end_date TIMESTAMP
--;;
ALTER TABLE trap_station_session_camera DROP COLUMN trap_station_session_camera_created
--;;
ALTER TABLE trap_station_session_camera ADD COLUMN trap_station_session_camera_created TIMESTAMP
--;;
ALTER TABLE trap_station_session_camera DROP COLUMN trap_station_session_camera_updated
--;;
ALTER TABLE trap_station_session_camera ADD COLUMN trap_station_session_camera_updated TIMESTAMP
--;;
ALTER TABLE media DROP COLUMN media_created
--;;
ALTER TABLE media ADD COLUMN media_created TIMESTAMP
--;;
ALTER TABLE media DROP COLUMN media_updated
--;;
ALTER TABLE media ADD COLUMN media_updated TIMESTAMP
--;;
ALTER TABLE media DROP COLUMN media_capture_timestamp
--;;
ALTER TABLE media ADD COLUMN media_capture_timestamp TIMESTAMP
--;;
ALTER TABLE photo DROP COLUMN photo_created
--;;
ALTER TABLE photo ADD COLUMN photo_created TIMESTAMP
--;;
ALTER TABLE photo DROP COLUMN photo_updated
--;;
ALTER TABLE photo ADD COLUMN photo_updated TIMESTAMP
--;;
ALTER TABLE taxonomy DROP COLUMN taxonomy_created
--;;
ALTER TABLE taxonomy ADD COLUMN taxonomy_created TIMESTAMP
--;;
ALTER TABLE taxonomy DROP COLUMN taxonomy_updated
--;;
ALTER TABLE taxonomy ADD COLUMN taxonomy_updated TIMESTAMP
--;;
ALTER TABLE sighting DROP COLUMN sighting_created
--;;
ALTER TABLE sighting ADD COLUMN sighting_created TIMESTAMP
--;;
ALTER TABLE sighting DROP COLUMN sighting_updated
--;;
ALTER TABLE sighting ADD COLUMN sighting_updated TIMESTAMP
--;;
ALTER TABLE survey_taxonomy DROP COLUMN survey_taxonomy_created
--;;
ALTER TABLE survey_taxonomy ADD COLUMN survey_taxonomy_created TIMESTAMP
--;;
ALTER TABLE survey_taxonomy DROP COLUMN survey_taxonomy_updated
--;;
ALTER TABLE survey_taxonomy ADD COLUMN survey_taxonomy_updated TIMESTAMP
--;;
ALTER TABLE survey_file DROP COLUMN survey_file_created
--;;
ALTER TABLE survey_file ADD COLUMN survey_file_created TIMESTAMP
--;;
ALTER TABLE survey_file DROP COLUMN survey_file_updated
--;;
ALTER TABLE survey_file ADD COLUMN survey_file_updated TIMESTAMP
--;;
ALTER TABLE survey ADD COLUMN survey_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE survey ADD COLUMN survey_updated_ms BIGINT
--;;
ALTER TABLE site ADD COLUMN site_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE site ADD COLUMN site_updated_ms BIGINT
--;;
ALTER TABLE survey_site ADD COLUMN survey_site_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE survey_site ADD COLUMN survey_site_updated_ms BIGINT
--;;
ALTER TABLE camera ADD COLUMN camera_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE camera ADD COLUMN camera_updated_ms BIGINT
--;;
ALTER TABLE trap_station ADD COLUMN trap_station_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE trap_station ADD COLUMN trap_station_updated_ms BIGINT
--;;
ALTER TABLE trap_station_session ADD COLUMN trap_station_session_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE trap_station_session ADD COLUMN trap_station_session_updated_ms BIGINT
--;;
ALTER TABLE trap_station_session ADD COLUMN trap_station_session_start_date_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE trap_station_session ADD COLUMN trap_station_session_end_date_ms BIGINT
--;;
ALTER TABLE trap_station_session_camera ADD COLUMN trap_station_session_camera_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE trap_station_session_camera ADD COLUMN trap_station_session_camera_updated_ms BIGINT
--;;
ALTER TABLE media ADD COLUMN media_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE media ADD COLUMN media_updated_ms BIGINT
--;;
ALTER TABLE media ADD COLUMN media_capture_timestamp_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE photo ADD COLUMN photo_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE photo ADD COLUMN photo_updated_ms BIGINT
--;;
ALTER TABLE taxonomy ADD COLUMN taxonomy_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE taxonomy ADD COLUMN taxonomy_updated_ms BIGINT
--;;
ALTER TABLE sighting ADD COLUMN sighting_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE sighting ADD COLUMN sighting_updated_ms BIGINT
--;;
ALTER TABLE survey_taxonomy ADD COLUMN survey_taxonomy_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE survey_taxonomy ADD COLUMN survey_taxonomy_updated_ms BIGINT
--;;
ALTER TABLE survey_file ADD COLUMN survey_file_created_ms BIGINT NOT NULL DEFAULT 0
--;;
ALTER TABLE survey_file ADD COLUMN survey_file_updated_ms BIGINT
