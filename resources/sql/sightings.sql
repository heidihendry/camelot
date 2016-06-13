-- name: -create<!
INSERT INTO sighting (sighting_created, sighting_updated, sighting_quantity,
       species_id, media_id)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :sighting_quantity,
       :species_id, :media_id)

-- name: -get-specific
SELECT sighting_id, sighting_created, sighting_updated, sighting_quantity,
       species_id, media_id, species_scientific_name
FROM sighting
LEFT JOIN species USING (species_id)
WHERE sighting_id = :sighting_id

-- name: -get-all
SELECT sighting_id, sighting_created, sighting_updated, sighting_quantity,
       species_id, media_id, species_scientific_name
FROM sighting
LEFT JOIN species USING (species_id)
WHERE media_id = :media_id

-- name: -update!
UPDATE sighting
SET sighting_updated = CURRENT_TIMESTAMP,
    sighting_quantity = :sighting_quantity,
    species_id = :species_id,
    media_id = :media_id
WHERE sighting_id = :sighting_id

-- name: -delete!
DELETE FROM sighting
WHERE sighting_id = :sighting_id

-- name: -get-available
SELECT species_id, species_scientific_name
FROM species
WHERE species_id NOT IN (SELECT species_id
                      FROM sighting
                      WHERE sighting_id = :sighting_id)

-- name: -get-alternatives
SELECT species_id, species_scientific_name
FROM species
WHERE species_id NOT IN (SELECT species_id
                      FROM sighting
                      WHERE sighting_id = :sighting_id) OR species_id = :species_id

-- name: -get-all*
SELECT sighting_id, sighting_created, sighting_updated, sighting_quantity,
       species_id, media_id, species_scientific_name
FROM sighting
LEFT JOIN species USING (species_id)
LEFT JOIN media USING (media_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
