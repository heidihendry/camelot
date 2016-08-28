CREATE TABLE survey_taxonomy (
       survey_taxonomy_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       survey_taxonomy_created   TIMESTAMP NOT NULL,
       survey_taxonomy_updated   TIMESTAMP NOT NULL,
       survey_id                 INT NOT NULL REFERENCES survey ON DELETE CASCADE ON UPDATE RESTRICT,
       taxonomy_id               INT NOT NULL REFERENCES taxonomy ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (survey_taxonomy_id))
