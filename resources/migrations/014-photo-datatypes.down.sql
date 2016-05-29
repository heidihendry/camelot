ALTER TABLE photo DROP COLUMN photo_aperture_setting
--;;
ALTER TABLE photo ADD COLUMN photo_aperture_setting DECIMAL
--;;
ALTER TABLE photo DROP COLUMN photo_exposure_value
--;;
ALTER TABLE photo ADD COLUMN photo_exposure_value DECIMAL
