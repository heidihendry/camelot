CREATE TABLE survey_species (
       survey_species_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (start with 1, increment by 1),
       survey_species_created   TIMESTAMP NOT NULL,
       survey_species_updated   TIMESTAMP NOT NULL,
       survey_id                INT NOT NULL REFERENCES survey ON DELETE CASCADE ON UPDATE RESTRICT,
       species_id               INT NOT NULL REFERENCES species ON DELETE CASCADE ON UPDATE RESTRICT,
       PRIMARY KEY (survey_species_id))
--;;
ALTER TABLE sighting DROP COLUMN species_id
--;;
ALTER TABLE sighting ADD COLUMN sighting_species_id INT NOT NULL DEFAULT 0 REFERENCES survey_species ON DELETE CASCADE ON UPDATE RESTRICT
