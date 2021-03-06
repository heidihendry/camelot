CREATE TABLE sighting_field (
       sighting_field_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       sighting_field_created   BIGINT NOT NULL,
       sighting_field_updated   BIGINT NOT NULL,
       sighting_field_key       VARCHAR(255) NOT NULL,
       sighting_field_label     VARCHAR(255) NOT NULL,
       sighting_field_datatype  VARCHAR(63) NOT NULL,
       sighting_field_required  BOOLEAN NOT NULL DEFAULT false,
       sighting_field_default   VARCHAR(4095),
       sighting_field_affects_independence BOOLEAN NOT NULL DEFAULT true,
       sighting_field_ordering  INT NOT NULL DEFAULT 10,
       survey_id                INT NOT NULL REFERENCES survey ON DELETE CASCADE ON UPDATE RESTRICT,
       CONSTRAINT UNIQUE_FIELD_KEY UNIQUE (sighting_field_key, survey_id),
       PRIMARY KEY (sighting_field_id))
--;;
CREATE TABLE sighting_field_option (
       sighting_field_option_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       sighting_field_option_created   BIGINT NOT NULL,
       sighting_field_option_updated   BIGINT NOT NULL,
       sighting_field_option_label     VARCHAR(255) NOT NULL,
       sighting_field_id               INT NOT NULL REFERENCES sighting_field ON DELETE CASCADE ON UPDATE RESTRICT,
       CONSTRAINT UNIQUE_FIELD_OPTION UNIQUE (sighting_field_option_label, sighting_field_id),
       PRIMARY KEY (sighting_field_option_id))
--;;
CREATE TABLE sighting_field_value (
       sighting_field_value_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       sighting_field_value_created   BIGINT NOT NULL,
       sighting_field_value_updated   BIGINT NOT NULL,
       sighting_field_value_data      VARCHAR(4095) NOT NULL,
       sighting_field_id              INT NOT NULL REFERENCES sighting_field ON DELETE CASCADE ON UPDATE RESTRICT,
       sighting_id                    INT NOT NULL REFERENCES sighting ON DELETE CASCADE ON UPDATE RESTRICT,
       CONSTRAINT UNIQUE_SIGHTING_FIELD UNIQUE (sighting_id, sighting_field_id),
       PRIMARY KEY (sighting_field_value_id))
