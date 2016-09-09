-- name: -get-all
SELECT survey_file_id, survey_file_created, survey_file_updated, survey_file_name,
       survey_file_size, survey_id
FROM survey_file
WHERE survey_id = :survey_id

-- name: -get-specific
SELECT survey_file_id, survey_file_created, survey_file_updated, survey_file_name,
       survey_file_size, survey_id
FROM survey_file
WHERE survey_file_id = :survey_file_id

-- name: -get-specific-by-details
SELECT survey_file_id, survey_file_created, survey_file_updated, survey_file_name,
       survey_file_size, survey_id
FROM survey_file
WHERE survey_file_name = :survey_file_name
      AND survey_id = :survey_id

-- name: -create<!
INSERT INTO survey_file (survey_file_created, survey_file_updated, survey_file_name,
       survey_file_size, survey_id)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :survey_file_name, :survey_file_size,
       :survey_id)

-- name: -update!
UPDATE survey_file
SET survey_file_updated = CURRENT_TIMESTAMP,
    survey_file_size = :survey_file_size
WHERE survey_file_id = :survey_file_id

-- name: -delete!
DELETE FROM survey_file
WHERE survey_file_id = :survey_file_id
