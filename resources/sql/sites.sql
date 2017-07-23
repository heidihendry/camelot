-- name: -create<!
INSERT INTO site (site_name, site_created, site_updated, site_sublocation,
                  site_city, site_state_province, site_country, site_area, site_notes)
VALUES (:site_name, :current_timestamp, :current_timestamp, :site_sublocation,
        :site_city, :site_state_province, :site_country, :site_area, :site_notes)

-- name: -update!
UPDATE site
SET site_updated = :current_timestamp,
    site_name = :site_name,
    site_sublocation = :site_sublocation,
    site_city = :site_city,
    site_state_province = :site_state_province,
    site_country = :site_country,
    site_area = :site_area,
    site_notes = :site_notes
WHERE site_id = :site_id

-- name: -delete!
DELETE FROM site
WHERE site_id = :site_id

-- name: -get-specific-by-name
SELECT site_id, site_created, site_updated, site_name, site_sublocation, site_city,
       site_state_province, site_country, site_area, site_notes
FROM site
WHERE site_name = :site_name

-- name: -get-specific
SELECT site_id, site_created, site_updated, site_name, site_sublocation, site_city,
       site_state_province, site_country, site_area, site_notes
FROM site
WHERE site_id = :site_id

-- name: -get-all
SELECT site_id, site_created, site_updated, site_name, site_sublocation, site_city,
       site_state_province, site_country, site_area, site_notes
FROM site

-- name: -get-active-cameras
SELECT camera_id
FROM site
LEFT JOIN survey_site USING (site_id)
LEFT JOIN trap_station USING (survey_site_id)
LEFT JOIN trap_station_session USING (trap_station_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_id)
WHERE trap_station_session_end_date IS NULL AND
      site_id = :site_id
