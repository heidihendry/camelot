-- name: get-all
SELECT camera_status_id, camera_status_description, camera_status_is_deployed,
       camera_status_is_terminated
FROM camera_status
