-- name: -create<!
INSERT INTO camera (camera_name, camera_created, camera_updated, camera_status,
                  camera_make, camera_model, camera_notes)
VALUES (:camera_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :camera_status,
        :camera_make, :camera_model, :camera_notes)

-- name: -update!
--UPDATE survey
--SET survey_updated = CURRENT_TIMESTAMP,
--    survey_name = :name,
--    survey_directory = :directory
--WHERE survey_id = :id

-- name: -delete!
DELETE FROM camera
WHERE camera_id = :camera_id

-- name: -get-specific-by-name
SELECT camera_id, camera_name, camera_status, camera_make, camera_model,
       camera_notes
FROM camera
WHERE camera_name = :camera_name

-- name: -get-specific
SELECT camera_id, camera_name, camera_status, camera_make, camera_model,
       camera_notes
FROM camera
WHERE camera_id = :camera_id

-- name: -get-all
SELECT camera_id, camera_name, camera_status, camera_make, camera_model,
       camera_notes
FROM camera
