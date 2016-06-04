-- name: -get-sightings-for-survey
SELECT species.species_scientific_name,
       sighting.sighting_quantity,
       trap_station_session.trap_station_session_id,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       media.media_capture_timestamp,
       trap_station.trap_station_id
FROM survey_site
LEFT JOIN site USING (site_id)
LEFT JOIN trap_station USING (survey_site_id)
LEFT JOIN trap_station_session USING (trap_station_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_id)
LEFT JOIN media USING (trap_station_session_camera_id)
LEFT JOIN sightings USING (media_id)
LEFT JOIN species USING (species_id)
WHERE survey_site.survey_id = :survey_id

-- name: -get-sightings-for-trap-station
SELECT species.species_scientific_name,
       sighting.sighting_quantity,
       trap_station_session.trap_station_session_id,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       media.media_capture_timestamp,
       trap_station.trap_station_id
FROM trap_station
LEFT JOIN trap_station_session USING (trap_station_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_id)
LEFT OUTER JOIN media USING (trap_station_session_camera_id)
LEFT OUTER JOIN sighting USING (media_id)
LEFT OUTER JOIN species USING (species_id)
WHERE trap_station.trap_station_id = :trap_station_id

-- name: -get-sightings-for-survey-site
SELECT species.species_scientific_name,
       sighting.sighting_quantity,
       trap_station_session.trap_station_session_id,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       media.media_capture_timestamp,
       trap_station.trap_station_id
FROM survey_site
LEFT JOIN trap_station USING (survey_site_id)
LEFT JOIN trap_station_session USING (trap_station_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_id)
LEFT OUTER JOIN media USING (trap_station_session_camera_id)
LEFT OUTER JOIN sighting USING (media_id)
LEFT OUTER JOIN species USING (species_id)
WHERE survey_site.survey_site_id = :survey_site_id

-- name: -get-all-data-by-survey
SELECT survey.survey_id,
       survey.survey_name,
       survey.survey_directory,
       survey.survey_notes,
       site.site_id,
       site.site_name,
       site.site_sublocation,
       site.site_city,
       site.site_state_province,
       site.site_country,
       site.site_notes,
       survey_site.survey_site_id,
       species.species_scientific_name,
       species.species_common_name,
       species.species_notes,
       camera.camera_id,
       camera.camera_name,
       camera.camera_make,
       camera.camera_model,
       camera.camera_notes,
       trap_station.trap_station_id,
       trap_station.trap_station_name,
       trap_station.trap_station_longitude,
       trap_station.trap_station_latitude,
       trap_station.trap_station_altitude,
       trap_station.trap_station_notes,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       trap_station_session.trap_station_session_id,
       trap_station_session_camera.trap_station_session_camera_id,
       media.media_id,
       media.media_capture_timestamp,
       media.media_notes,
       media.media_filename,
       sighting.sighting_quantity,
       photo.photo_iso_setting,
       photo.photo_exposure_value,
       photo.photo_flash_setting,
       photo.photo_fnumber_setting,
       photo.photo_orientation,
       photo.photo_resolution_x,
       photo.photo_resolution_y
FROM survey
LEFT OUTER JOIN survey_site USING (survey_id)
LEFT OUTER JOIN site USING (site_id)
LEFT OUTER JOIN trap_station USING (survey_site_id)
LEFT OUTER JOIN trap_station_session USING (trap_station_id)
LEFT OUTER JOIN trap_station_session_camera USING (trap_station_session_id)
LEFT OUTER JOIN media USING (trap_station_session_camera_id)
LEFT OUTER JOIN sighting USING (media_id)
LEFT OUTER JOIN species USING (species_id)
LEFT OUTER JOIN camera USING (camera_id)
LEFT OUTER JOIN photo USING (media_id)

-- name: -get-all-data-by-site
SELECT survey.survey_id,
       survey.survey_name,
       survey.survey_directory,
       survey.survey_notes,
       site.site_id,
       site.site_name,
       site.site_sublocation,
       site.site_city,
       site.site_state_province,
       site.site_country,
       site.site_notes,
       survey_site.survey_site_id,
       species.species_scientific_name,
       species.species_common_name,
       species.species_notes,
       camera.camera_id,
       camera.camera_name,
       camera.camera_make,
       camera.camera_model,
       camera.camera_notes,
       trap_station.trap_station_id,
       trap_station.trap_station_name,
       trap_station.trap_station_longitude,
       trap_station.trap_station_latitude,
       trap_station.trap_station_altitude,
       trap_station.trap_station_notes,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       trap_station_session.trap_station_session_id,
       trap_station_session_camera.trap_station_session_camera_id,
       media.media_id,
       media.media_capture_timestamp,
       media.media_notes,
       media.media_filename,
       sighting.sighting_quantity,
       photo.photo_iso_setting,
       photo.photo_exposure_value,
       photo.photo_flash_setting,
       photo.photo_fnumber_setting,
       photo.photo_orientation,
       photo.photo_resolution_x,
       photo.photo_resolution_y
FROM site
LEFT OUTER JOIN survey_site USING (site_id)
LEFT OUTER JOIN survey USING (survey_id)
LEFT OUTER JOIN trap_station USING (survey_site_id)
LEFT OUTER JOIN trap_station_session USING (trap_station_id)
LEFT OUTER JOIN trap_station_session_camera USING (trap_station_session_id)
LEFT OUTER JOIN media USING (trap_station_session_camera_id)
LEFT OUTER JOIN sighting USING (media_id)
LEFT OUTER JOIN species USING (species_id)
LEFT OUTER JOIN camera USING (camera_id)
LEFT OUTER JOIN photo USING (media_id)

-- name: -get-all-data-by-species
SELECT survey.survey_name,
       survey.survey_directory,
       survey.survey_notes,
       site.site_id,
       site.site_name,
       site.site_sublocation,
       site.site_city,
       site.site_state_province,
       site.site_country,
       site.site_notes,
       survey_site.survey_site_id,
       species.species_scientific_name,
       species.species_common_name,
       species.species_notes,
       camera.camera_id,
       camera.camera_name,
       camera.camera_make,
       camera.camera_model,
       camera.camera_notes,
       trap_station.trap_station_id,
       trap_station.trap_station_name,
       trap_station.trap_station_longitude,
       trap_station.trap_station_latitude,
       trap_station.trap_station_altitude,
       trap_station.trap_station_notes,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       trap_station_session.trap_station_session_id,
       trap_station_session_camera.trap_station_session_camera_id,
       media.media_id,
       media.media_capture_timestamp,
       media.media_notes,
       media.media_filename,
       sighting.sighting_quantity,
       photo.photo_iso_setting,
       photo.photo_exposure_value,
       photo.photo_flash_setting,
       photo.photo_fnumber_setting,
       photo.photo_orientation,
       photo.photo_resolution_x,
       photo.photo_resolution_y
FROM species
LEFT OUTER JOIN sighting USING (species_id)
LEFT OUTER JOIN media USING (media_id)
LEFT OUTER JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT OUTER JOIN trap_station_session USING (trap_station_session_id)
LEFT OUTER JOIN trap_station USING (trap_station_id)
LEFT OUTER JOIN survey_site USING (survey_site_id)
LEFT OUTER JOIN site USING (site_id)
LEFT OUTER JOIN survey USING (survey_id)
LEFT OUTER JOIN camera USING (camera_id)
LEFT OUTER JOIN photo USING (media_id)

-- name: -get-all-data-by-camera
SELECT survey.survey_id,
       survey.survey_name,
       survey.survey_directory,
       survey.survey_notes,
       site.site_id,
       site.site_name,
       site.site_sublocation,
       site.site_city,
       site.site_state_province,
       site.site_country,
       site.site_notes,
       survey_site.survey_site_id,
       species.species_scientific_name,
       species.species_common_name,
       species.species_notes,
       camera.camera_id,
       camera.camera_name,
       camera.camera_make,
       camera.camera_model,
       camera.camera_notes,
       trap_station.trap_station_id,
       trap_station.trap_station_name,
       trap_station.trap_station_longitude,
       trap_station.trap_station_latitude,
       trap_station.trap_station_altitude,
       trap_station.trap_station_notes,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       trap_station_session.trap_station_session_id,
       trap_station_session_camera.trap_station_session_camera_id,
       media.media_id,
       media.media_capture_timestamp,
       media.media_notes,
       media.media_filename,
       sighting.sighting_quantity,
       photo.photo_iso_setting,
       photo.photo_exposure_value,
       photo.photo_flash_setting,
       photo.photo_fnumber_setting,
       photo.photo_orientation,
       photo.photo_resolution_x,
       photo.photo_resolution_y
FROM camera
LEFT OUTER JOIN trap_station_session_camera USING (camera_id)
LEFT OUTER JOIN trap_station_session USING (trap_station_session_id)
LEFT OUTER JOIN trap_station USING (trap_station_id)
LEFT OUTER JOIN survey_site USING (survey_site_id)
LEFT OUTER JOIN site USING (site_id)
LEFT OUTER JOIN survey USING (survey_id)
LEFT OUTER JOIN media USING (trap_station_session_camera_id)
LEFT OUTER JOIN sighting USING (media_id)
LEFT OUTER JOIN species USING (species_id)
LEFT OUTER JOIN photo USING (media_id)

-- name: -get-all-species
SELECT species.species_scientific_name
FROM species
