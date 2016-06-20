ALTER TABLE survey ADD COLUMN survey_sighting_independence_threshold DECIMAL NOT NULL DEFAULT 20
--;;
ALTER TABLE survey ALTER COLUMN survey_directory NULL
