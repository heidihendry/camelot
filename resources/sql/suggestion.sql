-- name: create<!
INSERT INTO suggestion (suggestion_created, suggestion_updated,
       suggestion_key, suggestion_label,
       suggestion_confidence, media_id, bounding_box_id)
VALUES (:current_timestamp, :current_timestamp,
        :suggestion_key, :suggestion_label,
        :suggestion_confidence, :media_id, :bounding_box_id)

-- name: get-specific
SELECT suggestion_id, suggestion_created, suggestion_updated, suggestion_key,
       suggestion_label, suggestion_confidence, media_id, bounding_box_id,
       bounding_box_dimension_type, bounding_box_min_x, bounding_box_min_y,
       bounding_box_width, bounding_box_height
FROM suggestion
LEFT JOIN bounding_box USING (bounding_box_id)
WHERE suggestion_id = :suggestion_id

-- name: get-all
SELECT suggestion_id, suggestion_created, suggestion_updated, suggestion_key,
       suggestion_label, suggestion_confidence, media_id, bounding_box_id,
       bounding_box_dimension_type, bounding_box_min_x, bounding_box_min_y,
       bounding_box_width, bounding_box_height
FROM suggestion
LEFT JOIN bounding_box USING (bounding_box_id)
WHERE media_id = :media_id

-- name: get-all-for-media-ids
SELECT suggestion_id, suggestion_created, suggestion_updated, suggestion_key,
       suggestion_label, suggestion_confidence, media_id, bounding_box_id,
       bounding_box_dimension_type, bounding_box_min_x, bounding_box_min_y,
       bounding_box_width, bounding_box_height
FROM suggestion
LEFT JOIN bounding_box USING (bounding_box_id)
WHERE media_id IN (:media_ids)

-- name: delete!
DELETE FROM suggestion
WHERE suggestion_id = :suggestion_id

-- name: delete-for-media-id!
DELETE FROM suggestion
WHERE media_id = :media_id

-- name: delete-with-bounding-box!
DELETE FROM suggestion
WHERE bounding_box_id = :bounding_box_id
