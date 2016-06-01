-- name: -get-sightings-for-survey
SELECT species.species_scientific_name,
       sighting.sighting_quantity,
       site.site_sublocation,
       site.site_city,
       trap_station_session.trap_station_session_id,
       trap_station_session.trap_station_session_start_date,
       trap_station_session.trap_station_session_end_date,
       media.media_capture_timestamp,
       trap_station.trap_station_id
FROM sighting
LEFT JOIN species USING (species_id)
LEFT JOIN media USING (media_id)
LEFT JOIN trap_station_session_camera USING (trap_station_session_camera_id)
LEFT JOIN trap_station_session USING (trap_station_session_id)
LEFT JOIN trap_station USING (trap_station_id)
LEFT JOIN survey_site USING (survey_site_id)
LEFT JOIN site USING (site_id)
WHERE survey_site.survey_id = :survey_id
