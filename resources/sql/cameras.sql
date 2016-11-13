-- name: -create<!
INSERT INTO camera (camera_name, camera_created, camera_updated, camera_status_id,
                  camera_make, camera_model, camera_notes)
VALUES (:camera_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :camera_status_id,
        :camera_make, :camera_model, :camera_notes)

-- name: -update!
UPDATE camera
SET camera_updated = CURRENT_TIMESTAMP,
    camera_name = :camera_name,
    camera_make = :camera_make,
    camera_model = :camera_model,
    camera_status_id = :camera_status_id,
    camera_notes = :camera_notes
WHERE camera_id = :camera_id

-- name: -delete!
DELETE FROM camera
WHERE camera_id = :camera_id

-- name: -get-specific-by-name
SELECT camera_id, camera_created, camera_updated, camera_name, camera_status_id, camera_make,
       camera_model, camera_notes, camera_status_description, camera_status_description
FROM camera
LEFT JOIN camera_status USING (camera_status_id)
WHERE camera_name = :camera_name

-- name: -get-specific
SELECT camera_id, camera_created, camera_updated, camera_name, camera_status_id,
       camera_make, camera_model, camera_notes, camera_status_description
FROM camera
LEFT JOIN camera_status USING (camera_status_id)
WHERE camera_id = :camera_id

-- name: -get-all
SELECT camera_id, camera_created, camera_updated, camera_name, camera_status_id,
       camera_make, camera_model, camera_notes, camera_status_description
FROM camera
LEFT JOIN camera_status USING (camera_status_id)

-- name: -get-available
SELECT camera_id, camera_created, camera_updated, camera_name, camera_status_id,
       camera_make, camera_model, camera_notes, camera_status_description
FROM camera
LEFT JOIN camera_status USING (camera_status_id)
WHERE camera_status_description = 'camera-status/available'

-- name: -set-camera-status!
UPDATE camera
       SET camera_status_id = :camera_status_id,
           camera_updated = CURRENT_TIMESTAMP
WHERE camera_id = :camera_id
