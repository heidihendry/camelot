-- name: -create<!
INSERT INTO survey (survey_name, survey_created, survey_updated, survey_directory)
VALUES (:survey_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :survey_directory)

-- name: -update!
UPDATE survey
SET survey_updated = CURRENT_TIMESTAMP,
    survey_name = :survey_name,
    survey_directory = :survey_directory
WHERE survey_id = :survey_id

-- name: -delete!
DELETE FROM survey
WHERE survey_id = :survey_id

-- name: -get-specific-by-name
SELECT survey_id, survey_name, survey_directory
FROM survey
WHERE survey_name = :survey_name

-- name: -get-specific
SELECT survey_id, survey_name, survey_directory
FROM survey
WHERE survey_id = :survey_id

-- name: -get-all
SELECT survey_id, survey_name, survey_directory
FROM survey
