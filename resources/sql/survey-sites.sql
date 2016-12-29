-- name: -create<!
INSERT INTO survey_site (survey_id, survey_site_created, survey_site_updated, site_id)
VALUES (:survey_id, :current_timestamp, :current_timestamp, :site_id)

-- name: -update!
UPDATE survey_site
SET survey_site_updated = :current_timestamp,
    site_id = :site_id,
    survey_id = :survey_id
WHERE survey_site_id = :survey_site_id

-- name: -delete!
DELETE FROM survey_site
WHERE survey_site_id = :survey_site_id

-- name: -get-specific
SELECT survey_site_id, survey_site_created, survey_site_updated, site_id, survey_id, site_name
FROM survey_site
LEFT JOIN site using (site_id)
WHERE survey_site_id = :survey_site_id

-- name: -get-specific-by-site
SELECT survey_site_id, survey_site_created, survey_site_updated, site_id, survey_id, site_name
FROM survey_site
LEFT JOIN site using (site_id)
WHERE site_id = :site_id
      AND survey_id = :survey_id

-- name: -get-all
SELECT survey_site_id, survey_site_created, survey_site_updated, site_id, survey_id, site_name
FROM survey_site
LEFT JOIN site using (site_id)
WHERE survey_id = :survey_id

-- name: -get-all*
SELECT survey_site_id, survey_site_created, survey_site_updated, site_id, survey_id, site_name
FROM survey_site
LEFT JOIN site using (site_id)

-- name: -get-available
SELECT site_id, site_name
FROM site
WHERE site_id NOT IN (SELECT site_id
                      FROM survey_site
                      WHERE survey_id = :survey_id)

-- name: -get-alternatives
SELECT site_id, site_name
FROM site
WHERE site_id NOT IN (SELECT site_id
                      FROM survey_site
                      WHERE survey_id = :survey_id) OR site_id = :site_id
