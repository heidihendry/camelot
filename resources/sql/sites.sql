-- name: -create<!
INSERT INTO site (site_name, site_created, site_updated, site_sublocation,
                  site_city, site_state_province, site_country, site_notes)
VALUES (:site_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :site_sublocation,
        :site_city, :site_state_province, :site_country, :site_notes)

-- name: -update!
UPDATE site
SET site_updated = CURRENT_TIMESTAMP,
    site_name = :site_name,
    site_sublocation = :site_sublocation,
    site_city = :site_city,
    site_state_province = :site_state_province,
    site_country = :site_country,
    site_notes = :site_notes
WHERE site_id = :site_id

-- name: -delete!
DELETE FROM site
WHERE site_id = :site_id

-- name: -get-specific-by-name
SELECT site_id, site_name, site_sublocation, site_city, site_state_province,
       site_country, site_notes
FROM site
WHERE site_name = :site_name

-- name: -get-specific
SELECT site_id, site_name, site_sublocation, site_city, site_state_province,
       site_country, site_notes
FROM site
WHERE site_id = :site_id

-- name: -get-all
SELECT site_id, site_name, site_sublocation, site_city, site_state_province,
       site_country, site_notes
FROM site
