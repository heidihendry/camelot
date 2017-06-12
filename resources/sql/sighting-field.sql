-- name: -get-all
SELECT sighting_field_id, sighting_field_created, sighting_field_updated,
       sighting_field_key, sighting_field_label,
       sighting_field_datatype, sighting_field_required,
       sighting_field_default, sighting_field_affects_independence,
       sighting_field_ordering, survey_id
FROM sighting_field

-- name: -get-specific
SELECT sighting_field_id, sighting_field_created, sighting_field_updated,
       sighting_field_key, sighting_field_label,
       sighting_field_datatype, sighting_field_required,
       sighting_field_default, sighting_field_affects_independence,
       sighting_field_ordering, survey_id
FROM sighting_field
WHERE sighting_field_id = :sighting_field_id

-- name: -update!
UPDATE sighting_field
SET sighting_field_updated = :current_timestamp,
    sighting_field_key = :sighting_field_key,
    sighting_field_label = :sighting_field_label,
    sighting_field_datatype = :sighting_field_datatype,
    sighting_field_required = :sighting_field_required,
    sighting_field_default = :sighting_field_default,
    sighting_field_affects_independence = :sighting_field_affects_independence,
    sighting_field_ordering = :sighting_field_ordering
WHERE sighting_field_id = :sighting_field_id

-- name: -create<!
INSERT INTO sighting_field (sighting_field_created, sighting_field_updated,
       sighting_field_key, sighting_field_label,
       sighting_field_datatype, sighting_field_required,
       sighting_field_default, sighting_field_affects_independence,
       sighting_field_ordering, survey_id)
VALUES (:current_timestamp, :current_timestamp,
       :sighting_field_key, :sighting_field_label,
       :sighting_field_datatype, :sighting_field_required,
       :sighting_field_default, :sighting_field_affects_independence,
       :sighting_field_ordering, :survey_id)

-- name: -create-option<!
INSERT INTO sighting_field_option (
       sighting_field_option_created, sighting_field_option_updated,
       sighting_field_option_label, sighting_field_option_visible,
       sighting_field_id, survey_id)
VALUES (:current_timestamp, :current_timestamp, :sighting_field_option_label,
        :sighting_field_option_visible, :sighting_field_id, :survey_id)

-- name: -delete!
DELETE FROM sighting_field
WHERE sighting_field_id = :sighting_field_id

