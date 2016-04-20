-- name: -create<!
INSERT INTO survey (survey_name, survey_created, survey_updated, survey_directory)
VALUES (:name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :directory)

-- name: -update!
UPDATE survey
SET survey_updated = CURRENT_TIMESTAMP,
    survey_name = :name,
    survey_directory = :directory
WHERE survey_id = :id

-- name: -delete!
DELETE FROM survey
WHERE survey_id = :id

-- name: -get-specific
SELECT survey_id, survey_name, survey_directory
FROM survey
WHERE survey_id = :id

-- name: -get-all
SELECT survey_id, survey_name, survey_directory
FROM survey
