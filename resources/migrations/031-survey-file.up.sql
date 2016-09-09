CREATE TABLE survey_file (
       survey_file_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       survey_file_created   TIMESTAMP NOT NULL,
       survey_file_updated   TIMESTAMP NOT NULL,
       survey_file_name      VARCHAR(255) NOT NULL,
       survey_file_size      INT NOT NULL,
       survey_id             INT NOT NULL REFERENCES survey ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (survey_file_id))
