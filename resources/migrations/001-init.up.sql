CREATE TABLE survey (
       survey_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       survey_name      VARCHAR(255) UNIQUE NOT NULL,
       survey_created   TIMESTAMP NOT NULL,
       survey_updated   TIMESTAMP NOT NULL,
       survey_directory VARCHAR(1024) NOT NULL,
       PRIMARY KEY (survey_id))
--;;
CREATE TABLE site (
       site_id              INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       site_name            VARCHAR(255) NOT NULL,
       site_created         TIMESTAMP NOT NULL,
       site_updated         TIMESTAMP NOT NULL,
       site_state_province  VARCHAR(128),
       site_country         VARCHAR(128),
       PRIMARY KEY (site_id))
--;;
CREATE TABLE survey_site (
       survey_site_id       INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       survey_site_created  TIMESTAMP NOT NULL,
       survey_site_updated  TIMESTAMP NOT NULL,
       survey_id            INT NOT NULL REFERENCES survey ON DELETE CASCADE ON UPDATE RESTRICT,
       site_id              INT NOT NULL REFERENCES site ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (survey_site_id))
--;;
CREATE TABLE camera_status (
       camera_status_id               INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       camera_status_is_deployed      BOOLEAN NOT NULL,
       camera_status_is_terminated    BOOLEAN NOT NULL,
       camera_status_description      VARCHAR(32) NOT NULL,
       survey_id                      INT REFERENCES survey ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (camera_status_id))
--;;
CREATE TABLE camera (
       camera_id                INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       camera_name              VARCHAR(32) UNIQUE NOT NULL,
       camera_created           TIMESTAMP NOT NULL,
       camera_updated           TIMESTAMP NOT NULL,
       camera_status            INT NOT NULL REFERENCES camera_status ON DELETE CASCADE ON UPDATE RESTRICT,
       camera_make              VARCHAR(32),
       camera_model             VARCHAR(32),
       camera_software_version  VARCHAR(32),
       camera_notes             LONG VARCHAR,
       PRIMARY KEY (camera_id))
--;;
CREATE TABLE trap_station (
       trap_station_id          INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       trap_station_name        VARCHAR(255) NOT NULL,
       trap_station_created     TIMESTAMP NOT NULL,
       trap_station_updated     TIMESTAMP NOT NULL,
       trap_station_longitude   DECIMAL(9,6) NOT NULL,
       trap_station_latitude    DECIMAL(9,6) NOT NULL,
       trap_station_altitude    INT,
       trap_station_sublocation VARCHAR(255),
       trap_station_notes       LONG VARCHAR,
       survey_site_id           INT NOT NULL REFERENCES survey_site ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (trap_station_id))
--;;
CREATE TABLE trap_station_session (
       trap_station_session_id          INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       trap_station_session_start_date  TIMESTAMP NOT NULL,
       trap_station_session_end_date    TIMESTAMP,
       trap_station_notes               LONG VARCHAR,
       trap_station_id                  INT NOT NULL REFERENCES trap_station ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (trap_station_session_id))
--;;
CREATE TABLE trap_station_session_camera (
       trap_station_session_camera_id       INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       trap_station_session_camera_created  TIMESTAMP NOT NULL,
       trap_station_session_camera_updated  TIMESTAMP NOT NULL,
       camera_id                            INT NOT NULL REFERENCES camera ON DELETE CASCADE ON UPDATE RESTRICT,
       trap_station_session_id              INT NOT NULL REFERENCES trap_station_session ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (trap_station_session_camera_id))
--;;
CREATE TABLE media (
       media_id                         INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       media_created                    TIMESTAMP NOT NULL,
       media_updated                    TIMESTAMP NOT NULL,
       media_capture_timestamp          TIMESTAMP NOT NULL,
       media_pathname                   VARCHAR(1024) NOT NULL,
       media_cameracheck                BOOLEAN NOT NULL DEFAULT false,
       media_attention_needed           BOOLEAN NOT NULL DEFAULT false,
       media_notes                      LONG VARCHAR,
       trap_station_session_camera_id   INT NOT NULL REFERENCES trap_station_session_camera ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (media_id))
--;;
CREATE TABLE photo (
       photo_id                 INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       photo_created            TIMESTAMP NOT NULL,
       photo_updated            TIMESTAMP NOT NULL,
       photo_iso_setting        INT,
       photo_aperture_setting   DECIMAL,
       photo_exposure_value     DECIMAL,
       photo_flash_setting      VARCHAR(255),
       photo_focal_length       VARCHAR(16),
       photo_fnumber_setting    VARCHAR(16),
       photo_orientation        VARCHAR(128),
       photo_resolution_x       INT,
       photo_resolution_y       INT,
       media_id                 INT NOT NULL REFERENCES media ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (photo_id))
--;;
CREATE TABLE species (
       species_id               INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       species_scientific_name  VARCHAR(255) NOT NULL,
       species_common_name      VARCHAR(255) NOT NULL,
       species_created          TIMESTAMP NOT NULL,
       species_updated          TIMESTAMP NOT NULL,
       species_notes            LONG VARCHAR,
       PRIMARY KEY (species_id))
--;;
CREATE TABLE survey_species (
       survey_species_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       survey_species_created   TIMESTAMP NOT NULL,
       survey_species_updated   TIMESTAMP NOT NULL,
       survey_id                INT NOT NULL REFERENCES survey ON DELETE CASCADE ON UPDATE RESTRICT,
       species_id               INT NOT NULL REFERENCES species ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (survey_species_id))
--;;
CREATE TABLE sighting (
       sighting_id              INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 0, increment by 1),
       sighting_created         TIMESTAMP NOT NULL,
       sighting_updated         TIMESTAMP NOT NULL,
       sighting_quantity        INT NOT NULL,
       sighting_species_id      INT NOT NULL REFERENCES survey_species ON DELETE CASCADE ON UPDATE RESTRICT,
       media_id                 INT NOT NULL REFERENCES media ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (sighting_id))
