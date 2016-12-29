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
