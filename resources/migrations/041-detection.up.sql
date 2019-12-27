CREATE TABLE detection (
       detection_id         INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       detection_created    BIGINT NOT NULL,
       detection_updated    BIGINT NOT NULL,
       detection_type       VARCHAR(255) NOT NULL,
       detection_confidence REAL NOT NULL,
       detection_rel_min_x  REAL,
       detection_rel_min_y  REAL,
       detection_rel_width  REAL,
       detection_rel_height REAL,
       media_id             INT NOT NULL REFERENCES media ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (detection_id))
--;;
ALTER TABLE survey ADD COLUMN survey_detector VARCHAR(255)
