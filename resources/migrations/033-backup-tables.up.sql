CREATE TABLE survey_migration_033 AS SELECT * FROM survey WITH NO DATA
--;;
INSERT INTO survey_migration_033 SELECT * FROM survey
--;;
CREATE TABLE site_migration_033 AS SELECT * FROM site WITH NO DATA
--;;
INSERT INTO site_migration_033 SELECT * FROM site
--;;
CREATE TABLE survey_site_migration_033 AS SELECT * FROM survey_site WITH NO DATA
--;;
INSERT INTO survey_site_migration_033 SELECT * FROM survey_site
--;;
CREATE TABLE camera_migration_033 AS SELECT * FROM camera WITH NO DATA
--;;
INSERT INTO camera_migration_033 SELECT * FROM camera
--;;
CREATE TABLE trap_station_migration_033 AS SELECT * FROM trap_station WITH NO DATA
--;;
INSERT INTO trap_station_migration_033 SELECT * FROM trap_station
--;;
CREATE TABLE trap_station_session_migration_033 AS SELECT * FROM trap_station_session WITH NO DATA
--;;
INSERT INTO trap_station_session_migration_033 SELECT * FROM trap_station_session
--;;
CREATE TABLE trap_station_session_camera_migration_033 AS SELECT * FROM trap_station_session_camera WITH NO DATA
--;;
INSERT INTO trap_station_session_camera_migration_033 SELECT * FROM trap_station_session_camera
--;;
CREATE TABLE media_migration_033 AS SELECT * FROM media WITH NO DATA
--;;
INSERT INTO media_migration_033 SELECT * FROM media
--;;
CREATE TABLE photo_migration_033 AS SELECT * FROM photo WITH NO DATA
--;;
INSERT INTO photo_migration_033 SELECT * FROM photo
--;;
CREATE TABLE taxonomy_migration_033 AS SELECT * FROM taxonomy WITH NO DATA
--;;
INSERT INTO taxonomy_migration_033 SELECT * FROM taxonomy
--;;
CREATE TABLE sighting_migration_033 AS SELECT * FROM sighting WITH NO DATA
--;;
INSERT INTO sighting_migration_033 SELECT * FROM sighting
--;;
CREATE TABLE survey_taxonomy_migration_033 AS SELECT * FROM survey_taxonomy WITH NO DATA
--;;
INSERT INTO survey_taxonomy_migration_033 SELECT * FROM survey_taxonomy
--;;
CREATE TABLE survey_file_migration_033 AS SELECT * FROM survey_file WITH NO DATA
--;;
INSERT INTO survey_file_migration_033 SELECT * FROM survey_file
