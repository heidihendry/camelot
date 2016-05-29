ALTER TABLE photo DROP COLUMN photo_aperture_setting
--;;
ALTER TABLE photo ADD COLUMN photo_aperture_setting VARCHAR(128)
--;;
ALTER TABLE photo DROP COLUMN photo_exposure_value
--;;
ALTER TABLE photo ADD COLUMN photo_exposure_value VARCHAR(128)
