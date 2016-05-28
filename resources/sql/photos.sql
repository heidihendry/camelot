-- name: -create<!
INSERT INTO photo (photo_created, photo_updated, photo_iso_setting,
       photo_aperture_setting, photo_exposure_value, photo_flash_setting,
       photo_focal_length, photo_fnumber_setting, photo_orientation,
       photo_resolution_x, photo_resolution_y, media_id)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :photo_iso_setting,
       :photo_aperture_setting, :photo_exposure_value, :photo_flash_setting,
       :photo_focal_length, :photo_fnumber_setting, :photo_orientation,
       :photo_resolution_x, :photo_resolution_y, :media_id)

-- name: -get-specific
SELECT photo_id, photo_created, photo_updated, photo_iso_setting,
       photo_aperture_setting, photo_exposure_value, photo_flash_setting,
       photo_focal_length, photo_fnumber_setting, photo_orientation,
       photo_resolution_x, photo_resolution_y, media_id
FROM photo
WHERE photo_id = :photo_id

-- name: -get-all
SELECT photo_id, photo_created, photo_updated, photo_iso_setting,
       photo_aperture_setting, photo_exposure_value, photo_flash_setting,
       photo_focal_length, photo_fnumber_setting, photo_orientation,
       photo_resolution_x, photo_resolution_y, media_id
FROM photo
WHERE media_id = :media_id
