-- name: -create<!
INSERT INTO photo (photo_created, photo_updated, photo_iso_setting,
       photo_exposure_value, photo_flash_setting,
       photo_focal_length, photo_fnumber_setting, photo_orientation,
       photo_resolution_x, photo_resolution_y, media_id)
VALUES (:current_timestamp, :current_timestamp, :photo_iso_setting,
       :photo_exposure_value, :photo_flash_setting,
       :photo_focal_length, :photo_fnumber_setting, :photo_orientation,
       :photo_resolution_x, :photo_resolution_y, :media_id)

-- name: -get-specific
SELECT photo_id, photo_created, photo_updated, photo_iso_setting,
       photo_exposure_value, photo_flash_setting,
       photo_focal_length, photo_fnumber_setting, photo_orientation,
       photo_resolution_x, photo_resolution_y, media_id
FROM photo
WHERE photo_id = :photo_id

-- name: -get-all
SELECT photo_id, photo_created, photo_updated, photo_iso_setting,
       photo_exposure_value, photo_flash_setting,
       photo_focal_length, photo_fnumber_setting, photo_orientation,
       photo_resolution_x, photo_resolution_y, media_id
FROM photo
WHERE media_id = :media_id

-- name: -get-with-media-ids
SELECT DISTINCT
       photo_id, photo_created, photo_updated, photo_iso_setting,
       photo_exposure_value, photo_flash_setting,
       photo_focal_length, photo_fnumber_setting, photo_orientation,
       photo_resolution_x, photo_resolution_y, media_id
FROM photo
WHERE media_id IN (:media_ids)

-- name: -get-all*
SELECT photo_id, photo_created, photo_updated, photo_iso_setting,
       photo_exposure_value, photo_flash_setting,
       photo_focal_length, photo_fnumber_setting, photo_orientation,
       photo_resolution_x, photo_resolution_y, media_id
FROM photo

-- name: -update!
UPDATE photo
SET photo_updated = :current_timestamp,
    photo_iso_setting = :photo_iso_setting,
    photo_exposure_value = :photo_exposure_value,
    photo_flash_setting = :photo_flash_setting,
    photo_focal_length = :photo_focal_length,
    photo_fnumber_setting = :photo_fnumber_setting,
    photo_orientation = :photo_orientation,
    photo_resolution_x = :photo_resolution_x,
    photo_resolution_y = :photo_resolution_y
WHERE photo_id = :photo_id

-- name: -delete!
DELETE FROM photo
WHERE photo_id = :photo_id
