-- name: -get-all-by-survey
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
       site.site_area,
       site.site_notes,
       survey_site.survey_site_id,
       taxonomy.taxonomy_id,
       taxonomy.taxonomy_species,
       taxonomy.taxonomy_genus,
       taxonomy.taxonomy_family,
       taxonomy.taxonomy_order,
       taxonomy.taxonomy_class,
       taxonomy.taxonomy_common_name,
       taxonomy.taxonomy_notes,
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
       media.media_filename,
       media.media_format,
       media.media_notes,
       sighting.sighting_quantity,
       sighting.sighting_sex,
       sighting.sighting_lifestage,
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
LEFT OUTER JOIN taxonomy USING (taxonomy_id)
LEFT OUTER JOIN camera USING (camera_id)
LEFT OUTER JOIN photo USING (media_id)

-- name: -get-all-by-site
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
       site.site_area,
       site.site_notes,
       survey_site.survey_site_id,
       taxonomy.taxonomy_id,
       taxonomy.taxonomy_species,
       taxonomy.taxonomy_genus,
       taxonomy.taxonomy_family,
       taxonomy.taxonomy_order,
       taxonomy.taxonomy_class,
       taxonomy.taxonomy_common_name,
       taxonomy.taxonomy_notes,
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
       media.media_filename,
       media.media_format,
       media.media_notes,
       sighting.sighting_quantity,
       sighting.sighting_sex,
       sighting.sighting_lifestage,
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
LEFT OUTER JOIN taxonomy USING (taxonomy_id)
LEFT OUTER JOIN camera USING (camera_id)
LEFT OUTER JOIN photo USING (media_id)

-- name: -get-all-by-taxonomy
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
       site.site_area,
       site.site_notes,
       survey_site.survey_site_id,
       taxonomy.taxonomy_id,
       taxonomy.taxonomy_species,
       taxonomy.taxonomy_genus,
       taxonomy.taxonomy_family,
       taxonomy.taxonomy_order,
       taxonomy.taxonomy_class,
       taxonomy.taxonomy_common_name,
       taxonomy.taxonomy_notes,
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
       media.media_filename,
       media.media_format,
       media.media_notes,
       sighting.sighting_quantity,
       sighting.sighting_sex,
       sighting.sighting_lifestage,
       photo.photo_iso_setting,
       photo.photo_exposure_value,
       photo.photo_flash_setting,
       photo.photo_fnumber_setting,
       photo.photo_orientation,
       photo.photo_resolution_x,
       photo.photo_resolution_y
FROM taxonomy
LEFT OUTER JOIN sighting USING (taxonomy_id)
LEFT OUTER JOIN media USING (media_id)
LEFT OUTER JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT OUTER JOIN trap_station_session USING (trap_station_session_id)
LEFT OUTER JOIN trap_station USING (trap_station_id)
LEFT OUTER JOIN survey_site USING (survey_site_id)
LEFT OUTER JOIN site USING (site_id)
LEFT OUTER JOIN survey USING (survey_id)
LEFT OUTER JOIN camera USING (camera_id)
LEFT OUTER JOIN photo USING (media_id)

-- name: -get-all-by-camera
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
       site.site_area,
       site.site_notes,
       survey_site.survey_site_id,
       taxonomy.taxonomy_id,
       taxonomy.taxonomy_species,
       taxonomy.taxonomy_genus,
       taxonomy.taxonomy_family,
       taxonomy.taxonomy_order,
       taxonomy.taxonomy_class,
       taxonomy.taxonomy_common_name,
       taxonomy.taxonomy_notes,
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
       media.media_filename,
       media.media_format,
       media.media_notes,
       sighting.sighting_quantity,
       sighting.sighting_sex,
       sighting.sighting_lifestage,
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
LEFT OUTER JOIN taxonomy USING (taxonomy_id)
LEFT OUTER JOIN photo USING (media_id)

-- name: -get-all
SELECT survey.survey_id,
       survey.survey_name,
       survey.survey_directory,
       site.site_id,
       site.site_name,
       site.site_sublocation,
       site.site_city,
       site.site_state_province,
       site.site_country,
       site.site_area,
       survey_site.survey_site_id,
       taxonomy.taxonomy_id,
       taxonomy.taxonomy_species,
       taxonomy.taxonomy_genus,
       taxonomy.taxonomy_family,
       taxonomy.taxonomy_order,
       taxonomy.taxonomy_class,
       taxonomy.taxonomy_common_name,
       camera.camera_id,
       camera.camera_name,
       camera.camera_make,
       camera.camera_model,
       trap_station.trap_station_id,
       trap_station.trap_station_name,
       trap_station.trap_station_longitude,
       trap_station.trap_station_latitude,
       trap_station.trap_station_altitude,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       trap_station_session.trap_station_session_id,
       trap_station_session_camera.trap_station_session_camera_id,
       media.media_id,
       media.media_capture_timestamp,
       media.media_filename,
       media.media_format,
       sighting.sighting_quantity,
       sighting.sighting_sex,
       sighting.sighting_lifestage,
       photo.photo_iso_setting,
       photo.photo_exposure_value,
       photo.photo_flash_setting,
       photo.photo_fnumber_setting,
       photo.photo_orientation,
       photo.photo_resolution_x,
       photo.photo_resolution_y
FROM taxonomy
LEFT OUTER JOIN sighting USING (taxonomy_id)
LEFT OUTER JOIN media USING (media_id)
LEFT OUTER JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT OUTER JOIN trap_station_session USING (trap_station_session_id)
LEFT OUTER JOIN trap_station USING (trap_station_id)
LEFT OUTER JOIN survey_site USING (survey_site_id)
LEFT OUTER JOIN site USING (site_id)
LEFT OUTER JOIN survey USING (survey_id)
LEFT OUTER JOIN camera USING (camera_id)
LEFT OUTER JOIN photo USING (media_id)
UNION
SELECT survey.survey_id,
       survey.survey_name,
       survey.survey_directory,
       site.site_id,
       site.site_name,
       site.site_sublocation,
       site.site_city,
       site.site_state_province,
       site.site_country,
       site.site_area,
       survey_site.survey_site_id,
       taxonomy.taxonomy_id,
       taxonomy.taxonomy_species,
       taxonomy.taxonomy_genus,
       taxonomy.taxonomy_family,
       taxonomy.taxonomy_order,
       taxonomy.taxonomy_class,
       taxonomy.taxonomy_common_name,
       camera.camera_id,
       camera.camera_name,
       camera.camera_make,
       camera.camera_model,
       trap_station.trap_station_id,
       trap_station.trap_station_name,
       trap_station.trap_station_longitude,
       trap_station.trap_station_latitude,
       trap_station.trap_station_altitude,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       trap_station_session.trap_station_session_id,
       trap_station_session_camera.trap_station_session_camera_id,
       media.media_id,
       media.media_capture_timestamp,
       media.media_filename,
       media.media_format,
       sighting.sighting_quantity,
       sighting.sighting_sex,
       sighting.sighting_lifestage,
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
LEFT OUTER JOIN taxonomy USING (taxonomy_id)
LEFT OUTER JOIN camera USING (camera_id)
LEFT OUTER JOIN photo USING (media_id)
