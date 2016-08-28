-- name: -create<!
INSERT INTO survey_taxonomy (survey_taxonomy_created,
       survey_taxonomy_updated, survey_id, taxonomy_id)
       VALUES
       (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :survey_id, :taxonomy_id)

-- name: -get-all
SELECT survey_id, taxonomy_id, survey_taxonomy_created,
       survey_taxonomy_updated, survey_taxonomy_id
FROM survey_taxonomy

-- name: -get-all-for-survey
SELECT survey_id, taxonomy_id, survey_taxonomy_created,
       survey_taxonomy_updated, survey_taxonomy_id
FROM survey_taxonomy
WHERE survey_id = :survey_id

-- name: -get-all-for-taxonomy
SELECT survey_id, taxonomy_id, survey_taxonomy_created,
       survey_taxonomy_updated, survey_taxonomy_id
FROM survey_taxonomy
WHERE taxonomy_id = :taxonomy_id

-- name: -get-specific
SELECT survey_id, taxonomy_id, survey_taxonomy_created,
       survey_taxonomy_updated, survey_taxonomy_id
FROM survey_taxonomy
WHERE survey_taxonomy_id = :survey_taxonomy_id

-- name: -get-specific-by-relations
SELECT survey_id, taxonomy_id, survey_taxonomy_created,
       survey_taxonomy_updated, survey_taxonomy_id
FROM survey_taxonomy
WHERE survey_id = :survey_id AND taxonomy_id = :taxonomy_id

-- name: -delete!
DELETE FROM survey_taxonomy
WHERE survey_taxonomy_id = :survey_taxonomy_id
