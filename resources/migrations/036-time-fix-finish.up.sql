ALTER TABLE survey DROP COLUMN survey_created
--;;
RENAME COLUMN survey.survey_created_ms TO survey_created
--;;
ALTER TABLE survey DROP COLUMN survey_updated
--;;
RENAME COLUMN survey.survey_updated_ms TO survey_updated
--;;
ALTER TABLE site DROP COLUMN site_created
--;;
RENAME COLUMN site.site_created_ms TO site_created
--;;
ALTER TABLE site DROP COLUMN site_updated
--;;
RENAME COLUMN site.site_updated_ms TO site_updated
--;;
ALTER TABLE survey_site DROP COLUMN survey_site_created
--;;
RENAME COLUMN survey_site.survey_site_created_ms TO survey_site_created
--;;
ALTER TABLE survey_site DROP COLUMN survey_site_updated
--;;
RENAME COLUMN survey_site.survey_site_updated_ms TO survey_site_updated
--;;
ALTER TABLE camera DROP COLUMN camera_created
--;;
RENAME COLUMN camera.camera_created_ms TO camera_created
--;;
ALTER TABLE camera DROP COLUMN camera_updated
--;;
RENAME COLUMN camera.camera_updated_ms TO camera_updated
--;;
ALTER TABLE trap_station DROP COLUMN trap_station_created
--;;
RENAME COLUMN trap_station.trap_station_created_ms TO trap_station_created
--;;
ALTER TABLE trap_station DROP COLUMN trap_station_updated
--;;
RENAME COLUMN trap_station.trap_station_updated_ms TO trap_station_updated
--;;
ALTER TABLE trap_station_session DROP COLUMN trap_station_session_created
--;;
RENAME COLUMN trap_station_session.trap_station_session_created_ms TO trap_station_session_created
--;;
ALTER TABLE trap_station_session DROP COLUMN trap_station_session_updated
--;;
RENAME COLUMN trap_station_session.trap_station_session_updated_ms TO trap_station_session_updated
--;;
ALTER TABLE trap_station_session DROP COLUMN trap_station_session_start_date
--;;
RENAME COLUMN trap_station_session.trap_station_session_start_date_ms TO trap_station_session_start_date
--;;
ALTER TABLE trap_station_session DROP COLUMN trap_station_session_end_date
--;;
RENAME COLUMN trap_station_session.trap_station_session_end_date_ms TO trap_station_session_end_date
--;;
ALTER TABLE trap_station_session_camera DROP COLUMN trap_station_session_camera_created
--;;
RENAME COLUMN trap_station_session_camera.trap_station_session_camera_created_ms TO trap_station_session_camera_created
--;;
ALTER TABLE trap_station_session_camera DROP COLUMN trap_station_session_camera_updated
--;;
RENAME COLUMN trap_station_session_camera.trap_station_session_camera_updated_ms TO trap_station_session_camera_updated
--;;
ALTER TABLE media DROP COLUMN media_created
--;;
RENAME COLUMN media.media_created_ms TO media_created
--;;
ALTER TABLE media DROP COLUMN media_updated
--;;
RENAME COLUMN media.media_updated_ms TO media_updated
--;;
ALTER TABLE media DROP COLUMN media_capture_timestamp
--;;
RENAME COLUMN media.media_capture_timestamp_ms TO media_capture_timestamp
--;;
ALTER TABLE photo DROP COLUMN photo_created
--;;
RENAME COLUMN photo.photo_created_ms TO photo_created
--;;
ALTER TABLE photo DROP COLUMN photo_updated
--;;
RENAME COLUMN photo.photo_updated_ms TO photo_updated
--;;
ALTER TABLE taxonomy DROP COLUMN taxonomy_created
--;;
RENAME COLUMN taxonomy.taxonomy_created_ms TO taxonomy_created
--;;
ALTER TABLE taxonomy DROP COLUMN taxonomy_updated
--;;
RENAME COLUMN taxonomy.taxonomy_updated_ms TO taxonomy_updated
--;;
ALTER TABLE sighting DROP COLUMN sighting_created
--;;
RENAME COLUMN sighting.sighting_created_ms TO sighting_created
--;;
ALTER TABLE sighting DROP COLUMN sighting_updated
--;;
RENAME COLUMN sighting.sighting_updated_ms TO sighting_updated
--;;
ALTER TABLE survey_taxonomy DROP COLUMN survey_taxonomy_created
--;;
RENAME COLUMN survey_taxonomy.survey_taxonomy_created_ms TO survey_taxonomy_created
--;;
ALTER TABLE survey_taxonomy DROP COLUMN survey_taxonomy_updated
--;;
RENAME COLUMN survey_taxonomy.survey_taxonomy_updated_ms TO survey_taxonomy_updated
--;;
ALTER TABLE survey_file DROP COLUMN survey_file_created
--;;
RENAME COLUMN survey_file.survey_file_created_ms TO survey_file_created
--;;
ALTER TABLE survey_file DROP COLUMN survey_file_updated
--;;
RENAME COLUMN survey_file.survey_file_updated_ms TO survey_file_updated
