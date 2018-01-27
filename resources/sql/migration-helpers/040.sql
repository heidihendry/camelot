-- name: -get-sightings-for-survey
SELECT sighting_id, sighting_created, sighting_updated, sighting_lifestage,
       sighting_sex, media_id
FROM sighting
LEFT JOIN taxonomy USING (taxonomy_id)
LEFT JOIN media USING (media_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
WHERE survey_id = :survey_id

-- name: -get-survey-ids
SELECT survey_id
FROM survey

-- name: -create-internal-sighting-field<!
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
       sighting_field_option_label, sighting_field_id)
VALUES (:current_timestamp, :current_timestamp,
       :sighting_field_option_label, :sighting_field_id)

-- name: -create-field-value<!
INSERT INTO sighting_field_value (sighting_field_value_created,
       sighting_field_value_updated, sighting_field_value_data,
       sighting_id, sighting_field_id)
VALUES (:current_timestamp, :current_timestamp,
        :sighting_field_value_data, :sighting_id,
        :sighting_field_id)

-- name: -delete-field!
DELETE FROM sighting_field
WHERE sighting_field_id = :sighting_field_id

-- name: -get-migrated-sighting-fields
SELECT sighting_field_id
FROM sighting_field
WHERE sighting_field_key IN (:sighting_field_keys)
