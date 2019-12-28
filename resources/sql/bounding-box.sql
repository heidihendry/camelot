-- name: create<!
INSERT INTO bounding_box (bounding_box_dimension_type, bounding_box_min_x,
       bounding_box_min_y, bounding_box_width, bounding_box_height)
VALUES (:bounding_box_dimension_type, :bounding_box_min_x, :bounding_box_min_y,
        :bounding_box_width, :bounding_box_height)

-- name: delete!
DELETE FROM bounding_box
WHERE bounding_box_id = :bounding_box_id
