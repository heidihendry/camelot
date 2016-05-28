-- name: -create<!
INSERT INTO sighting (sighting_created, sighting_updated, sighting_quantity,
       species_id, media_id)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :sighting_quantity,
       :species_id, :media_id)

-- name: -get-specific
SELECT sighting_id, sighting_created, sighting_updated, sighting_quantity,
       species_id, media_id
FROM sighting
WHERE sighting_id = :sighting_id

-- name: -get-all
SELECT sighting_id, sighting_created, sighting_updated, sighting_quantity,
       species_id, media_id
FROM sighting
WHERE media_id = :media_id
