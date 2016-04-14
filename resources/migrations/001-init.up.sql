CREATE TABLE survey (
       survey_id        SERIAL INT NOT NULL,
       survey_name      VARCHAR(255) UNIQUE NOT NULL,
       survey_created   TIMESTAMP NOT NULL,
       survey_updated   TIMESTAMP NOT NULL,
       survey_directory VARCHAR(1024) NOT NULL,
       CONSTRAINT PRIMARY KEY (serial_id)
);

CREATE TABLE site (
       site_id              SERIAL INT NOT NULL,
       site_name            VARCHAR(255) NOT NULL,
       site_created         TIMESTAMP NOT NULL,
       site_updated         TIMESTAMP NOT NULL,
       site_state_province  VARCHAR(128),
       site_country         VARCHAR(128),
       CONSTRAINT PRIMARY KEY (site_id)
);

CREATE TABLE survey_site (
       survey_site_id       SERIAL INT NOT NULL,
       survey_site_created  TIMESTAMP NOT NULL,
       survey_site_updated  TIMESTAMP NOT NULL,
       survey_id            INT REFERENCES survey,
       site_id              INT REFERENCES site,
       CONSTRAINT PRIMARY KEY (survey_site_id)
);


CREATE TABLE camera_status (
       camera_status_id               SERIAL INT NOT NULL,
       camera_status_is_deployed      BOOLEAN NOT NULL,
       camera_status_is_terminated    BOOLEAN NOT NULL,
       camera_status_description      VARCHAR(32) NOT NULL,
       CONSTRAINT PRIMARY KEY (camera_status_id)
);

INSERT INTO camera_status VALUES
       (1, true,  false, "Active"),
       (2, false, false, "Available"),
       (3, false, true,  "Lost"),
       (4, false, true,  "Destroyed"),
       (5, false, true,  "Retired");

CREATE TABLE camera (
       camera_id                SERIAL INT NOT NULL,
       camera_name              VARCHAR(32) UNIQUE NOT NULL,
       camera_created           TIMESTAMP NOT NULL,
       camera_updated           TIMESTAMP NOT NULL,
       camera_status            INT REFERENCES camera_status,
       camera_make              VARCHAR(32),
       camera_model             VARCHAR(32),
       camera_software_version  VARCHAR(32),
       camera_notes             LONG VARCHAR,
       CONSTRAINT PRIMARY KEY (camera_id)
);

CREATE TABLE trap_station (
       trap_station_id          SERIAL INT NOT NULL,
       trap_station_name        VARCHAR(255) NOT NULL,
       trap_station_created     TIMESTAMP NOT NULL,
       trap_station_updated     TIMESTAMP NOT NULL,
       trap_station_longitude   DECIMAL(9,6) NOT NULL,
       trap_station_latitude    DECIMAL(9,6) NOT NULL,
       trap_station_altitude    INT,
       trap_station_sublocation VARCHAR(255),
       trap_station_notes       LONG VARCHAR,
       survey_site_id           INT REFERENCES survey_site,
       CONSTRAINT PRIMARY KEY (trap_station_id)
);

CREATE TABLE trap_station_session (
       trap_station_session_id          SERIAL INT NOT NULL,
       trap_station_session_start_date  TIMESTAMP NOT NULL,
       trap_station_session_end_date    TIMESTAMP,
       trap_station_notes               LONG VARCHAR,
       trap_station_id                  INT REFERENCES trap_station,
       CONSTRAINT PRIMARY KEY (trap_station_session_id)
);

CREATE TABLE trap_station_session_camera (
       trap_station_session_camera_id       SERIAL INT NOT NULL,
       trap_station_session_camera_created  TIMESTAMP NOT NULL,
       trap_station_session_camera_updated  TIMESTAMP NOT NULL,
       camera_id                            INT REFERENCES camera,
       trap_station_session_id              INT REFERENCES trap_station_session,
       CONSTRAINT PRIMARY KEY (trap_station_camera_id)
);

CREATE TABLE media (
       media_id                         SERIAL INT NOT NULL,
       media_created                    TIMESTAMP NOT NULL,
       media_updated                    TIMESTAMP NOT NULL,
       media_capture_timestamp          TIMESTAMP NOT NULL,
       media_pathname                   VARCHAR(1024) NOT NULL,
       media_cameracheck                BOOLEAN NOT NULL DEFAULT false,
       media_attention_needed           BOOLEAN NOT NULL DEFAULT false,
       media_notes                      LONG VARCHAR,
       trap_station_session_camera_id   INT REFERENCES trap_station_session_camera,
       CONSTRAINT PRIMARY KEY (media_id)
);

CREATE TABLE photo (
       photo_id                 SERIAL INT NOT NULL,
       photo_created            TIMESTAMP NOT NULL,
       photo_updated            TIMESTAMP NOT NULL,
       photo_iso_setting        INT,
       photo_aperture_setting   INT,
       photo_exposure_value     INT,
       photo_flash_setting      VARCHAR(255),
       photo_focal_length       VARCHAR(16),
       photo_fnumber_setting    VARCHAR(16),
       photo_orientation        VARCHAR(128),
       photo_resolution_x       INT,
       photo_resolution_y       INT,
       media_id                 INT REFERENCES media,
       CONSTRAINT PRIMARY KEY (photo_id)
);

CREATE TABLE species (
       species_id               SERIAL INT NOT NULL,
       species_name             VARCHAR(255) NOT NULL,
       species_created          TIMESTAMP NOT NULL,
       species_updated          TIMESTAMP NOT NULL,
       species_notes            LONG VARCHAR,
       CONSTRAINT PRIMARY KEY (species)
);

CREATE TABLE survey_species (
       survey_species_id        SERIAL INT NOT NULL,
       survey_species_created   TIMESTAMP NOT NULL,
       survey_species_updated   TIMESTAMP NOT NULL,
       survey_id                INT REFERENCES survey,
       species_id               INT REFERENCES species,
       CONSTRAINT PRIMARY KEY (survey_species)
);

CREATE TABLE sighting (
       sighting_id              SERIAL INT NOT NULL,
       sighting_created         TIMESTAMP NOT NULL,
       sighting_updated         TIMESTAMP NOT NULL,
       sighting_quantity        INT,
       sighting_species_id      INT REFERENCES survey_species,
       media_id                 INT REFERENCES media,
       CONSTRAINT PRIMARY KEY (sighting_id)
);

CREATE TABLE db_migration (
       migration_id          SERIAL INT NOT NULL,
       migration_name        VARCHAR(255) NOT NULL,
       migration_applied     TIMESTAMP NOT NULL,
       migration_rolled_back TIMESTAMP,
       CONSTRAINT PRIMARY KEY (migration_id)
);

CREATE INDEX db_migration_name (migration_name DESC);

CREATE VIEW db_migration_active (migration_name)
  AS (SELECT migration_name FROM db_migration WHERE migration_rolled_back IS NULL);
