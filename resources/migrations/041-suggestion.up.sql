CREATE TABLE bounding_box (
       bounding_box_id                INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       bounding_box_dimension_type    VARCHAR(255) NOT NULL,
       bounding_box_min_x             REAL NOT NULL,
       bounding_box_min_y             REAL NOT NULL,
       bounding_box_width             REAL NOT NULL,
       bounding_box_height            REAL NOT NULL,
       PRIMARY KEY (bounding_box_id))
--;;
CREATE TABLE suggestion (
       suggestion_id         INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       suggestion_created    BIGINT NOT NULL,
       suggestion_updated    BIGINT NOT NULL,
       suggestion_key        VARCHAR(255),
       suggestion_label      VARCHAR(255),
       suggestion_confidence REAL NOT NULL,
       bounding_box_id       INT REFERENCES bounding_box ON DELETE CASCADE ON UPDATE RESTRICT,
       media_id              INT NOT NULL REFERENCES media ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (suggestion_id))
--;;
ALTER TABLE sighting ADD COLUMN bounding_box_id INTEGER REFERENCES bounding_box ON DELETE CASCADE ON UPDATE RESTRICT
