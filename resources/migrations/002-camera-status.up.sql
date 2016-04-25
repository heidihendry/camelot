INSERT INTO camera_status (camera_status_is_deployed, camera_status_is_terminated, camera_status_description)
       VALUES (false, false, 'camera-status/available'),
              (true, false, 'camera-status/active'),
              (false, true, 'camera-status/lost'),
              (false, true, 'camera-status/stolen'),
              (false, true, 'camera-status/retired')
