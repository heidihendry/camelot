-- name: query-all
SELECT sighting_field_value_data, sighting_id, sighting_field_datatype,
       sighting_field_key
FROM sighting_field_value
LEFT JOIN sighting_field USING (sighting_field_id)

-- name: get-all-by-sighting-ids
SELECT sighting_field_value_data, sighting_id, sighting_field_datatype,
       sighting_field_key
FROM sighting_field_value, sighting_field
WHERE sighting_id IN (:sighting_ids)
      AND sighting_field_value.sighting_field_id = sighting_field.sighting_field_id

-- name: get-specific
SELECT sighting_field_value_id, sighting_field_value_created,
       sighting_field_value_updated, sighting_field_value_data,
       sighting_field_id, sighting_id, sighting_field_datatype,
       sighting_field_key
FROM sighting_field_value
LEFT JOIN sighting_field USING (sighting_field_id)
WHERE sighting_field_value_id = :sighting_field_value_id

-- name: get-for-sighting
SELECT sighting_field_value_id, sighting_field_value_created,
       sighting_field_value_updated, sighting_field_value_data,
       sighting_field_id, sighting_id, sighting_field_datatype,
       sighting_field_key
FROM sighting_field_value
LEFT JOIN sighting_field USING (sighting_field_id)
WHERE sighting_id = :sighting_id

-- name: update!
UPDATE sighting_field_value
SET sighting_field_value_updated = :current_timestamp,
    sighting_field_value_data = :sighting_field_value_data
WHERE sighting_field_value_id = :sighting_field_value_id

-- name: create<!
INSERT INTO sighting_field_value (sighting_field_value_created,
       sighting_field_value_updated, sighting_field_value_data,
       sighting_id, sighting_field_id)
VALUES (:current_timestamp,
       :current_timestamp, :sighting_field_value_data,
       :sighting_id, :sighting_field_id)

-- name: delete-for-sighting!
DELETE FROM sighting_field_value
WHERE sighting_id = :sighting_id
