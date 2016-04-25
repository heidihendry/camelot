INSERT INTO camera_status (camera_status_is_deployed, camera_status_is_terminated, camera_status_description)
       VALUES (false, false, 'Available'),
              (true, false, 'Active'),
              (false, true, 'Lost'),
              (false, true, 'Stolen'),
              (false, true, 'Retired')
