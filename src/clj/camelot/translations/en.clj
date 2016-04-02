(ns camelot.translations.en)

(def t-en
  {:problems {:error "[ERROR] "
              :warn "[WARNING] "
              :info "[INFO] "
              :ignore "[IGNORE] "
              :okay "[OK] "
              :rename-field-not-found "Unable to resolve the following field selectors during photo renaming: '%s'"
              :rename-existing-conflict "%s: Not renaming as doing so may overwrite existing file '%s'"
              :rename-conflict "%s: Not performing renames, as not all results would be unique. A file with this named would be the result of renaming the following files:"
              :species-quantity-missing "%s: has species without a quantity"
              :species-name-missing "%s: has quantity without a species"
              :default-config-exists "A default configuration file already exists. Please delete '%s' and try again."
              :config-not-found "A configuration file was not found.  Please run camelot with the 'init' option and adjust the configuration file created."
              }

   :status {:rename-photos "Renaming photos in album: '%s'"
            :apply-rename "Renaming '%s' to '%s'..."}

   :checks {:starting "Running consistency checks..."
            :failure-notice "FAIL: %s:"
            :photo-stddev "The timestamp on some photos significantly exceeds the standard deviation"
            :project-dates "The timestamp on some photos lies outside of the project start and stop times"
            :time-light-sanity "The camera settings do not coincide with the timestamp on numerous photos"
            :camera-checks "The photos do not have a camera check at the beginning and end"
            :headline-consistency "The headline of the photos differs across photos"
            :required-fields "One or more required fields are missing from one or more photos"
            :album-has-data "Album must not be empty"
            :sighting-consistency "Mismatch found in sighting data: species without quantity or quantity without species"
            :surveyed-species "A species was identified which is not known to the survey"
            }
   :language {:en "English"
              :vn "Vietnamese"}

   :metadata {:location.gps-longitude "GPS Longitude"
              :location.gps-longitude-ref "GPS Longitude Reference"
              :location.gps-latitude "GPS Latitude"
              :location.gps-latitude-ref "GPS Latitude Reference"
              :location.gps-altitude "GPS Altitude"
              :location.gps-altitude-ref "GPS Altitude Reference"
              :location.sublocation "Sublocation"
              :location.city "City"
              :location.state-province "State/Province"
              :location.country "Country"
              :location.country-code "Country Code"
              :location.map-datum "GPS Map Datum"
              :camera-settings.aperture "Aperture Setting"
              :camera-settings.exposure "Exposure Setting"
              :camera-settings.flash "Flash Setting"
              :camera-settings.focal-length "Focal Length Setting"
              :camera-settings.fstop "F-Stop Setting"
              :camera-settings.iso "ISO Setting"
              :camera-settings.orientation "Orientation Setting"
              :camera-settings.resolution-x "X-Resolution"
              :camera-settings.resolution-y "Y-Resolution"
              :camera.make "Camera Make"
              :camera.model "Camera Model"
              :camera.software "Camera Software"
              :datetime "Date/Time"
              :headline "Headline"
              :artist "Artist"
              :phase "Phase"
              :copyright "Copyright"
              :description "Description"
              :filename "File Name"
              :filesize "File Size"}

   :default-config-created "A default configuration has been created in '%s'"

   :missing  "|Missing translation: [%1$s %2$s %3$s]|"})
