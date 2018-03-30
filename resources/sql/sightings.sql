-- name: create<!
INSERT INTO sighting (sighting_created, sighting_updated, sighting_quantity,
        taxonomy_id, media_id)
VALUES (:current_timestamp, :current_timestamp, :sighting_quantity,
        :taxonomy_id, :media_id)

-- name: get-specific
SELECT sighting_id, sighting_created, sighting_updated, sighting_quantity,
       taxonomy_id, media_id, taxonomy_genus,
       taxonomy_species
FROM sighting
LEFT JOIN taxonomy USING (taxonomy_id)
WHERE sighting_id = :sighting_id

-- name: get-all
SELECT sighting_id, sighting_created, sighting_updated, sighting_quantity,
       taxonomy_id, media_id, taxonomy_genus,
       taxonomy_species
FROM sighting
LEFT JOIN taxonomy USING (taxonomy_id)
WHERE media_id = :media_id

-- name: update!
UPDATE sighting
SET sighting_updated = :current_timestamp,
    sighting_quantity = :sighting_quantity,
    taxonomy_id = :taxonomy_id
WHERE sighting_id = :sighting_id

-- name: delete!
DELETE FROM sighting
WHERE sighting_id = :sighting_id

-- name: get-available
SELECT taxonomy_id, taxonomy_genus, taxonomy_species
FROM taxonomy
WHERE taxonomy_id NOT IN (SELECT taxonomy_id
                          FROM sighting
                          WHERE sighting_id = :sighting_id)

-- name: get-alternatives
SELECT taxonomy_id, taxonomy_genus, taxonomy_genus
FROM taxonomy
WHERE taxonomy_id NOT IN (SELECT taxonomy_id
                          FROM sighting
                          WHERE sighting_id = :sighting_id) OR taxonomy_id = :taxonomy_id

-- name: get-all*
SELECT sighting_id, sighting_created, sighting_updated, sighting_quantity,
       taxonomy_id, media_id, taxonomy_genus,
       taxonomy_species
FROM sighting
LEFT JOIN taxonomy USING (taxonomy_id)
LEFT JOIN media USING (media_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
