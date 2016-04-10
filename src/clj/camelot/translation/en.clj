(ns camelot.translation.en)

(def t-en
  {:problems {:error "[ERROR] "
              :warn "[WARNING] "
              :info "[INFO] "
              :ignore "[IGNORE] "
              :okay "[OK] "
              :root-path-missing "Please specify the absolute path to the Survey Directory in the settings panel."
              :root-path-not-found "The directory for the survey was not found or could not be read"
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
            :problem-without-reason "%s. The exact reason is not known."
            :stddev-before "The timestamp on '%s' is significantly before other timestamps"
            :stddev-after "The timestamp on '%s' is significantly after other timestamps"
            :project-date-before "The timestamp on '%s' is before the project start date"
            :project-date-after "The timestamp on '%s' is before the project start date"
            :time-light-sanity "The camera settings do not coincide with the timestamp on numerous photos"
            :camera-checks "The photos do not have a camera check at the beginning and end"
            :headline-consistency "The headline of the photos differs across photos (tested '%s' and '%s')"
            :required-fields "One or more required fields are missing from '%s'"
            :album-has-data "Album must not be empty"
            :sighting-consistency "Mismatch found in sighting data: species without quantity or quantity without species"
            :surveyed-species "A species was identified which is not known to the survey"
            :future-timestamp "The timestamp on '%s' is in the future"
            }
   :language {:en "English"
              :vn "Vietnamese"}

   :config {:erroneous-infrared-threshold {:label "Erroneous Infrared Threshold"
                                           :description "Value between 0.0 and 1.0 to set the treshold for date/time error detection"}
            :infrared-iso-value-threshold {:label "Infrared ISO Value Threshold"
                                           :description "ISO value of the photos beyond which it is considered 'night'"}
            :sighting-independence-minutes-threshold {:label "Sighting Independence Threshold (mins)"
                                                      :description "The minimum amount of minutes which must elapse between an initial sighting, and a subsequent sighting, for the new sighting to be considered independent"}
            :language {:label "Language"
                       :description "Interface language to use"}
            :root-path {:label "Survey Directory"
                        :description "Pathname to the root directory of this survey's photo data. This must be as it would appear to the system running the Camelot server process."}
            :night-start-hour {:label "Night Start Time"
                               :description "Hour beyond which it is considered night"}
            :night-end-hour {:label "Night End Time"
                             :description "Hour at which it is considered daylight"}
            :project-start {:label "Project Start Date"
                            :description "The date which the project commenced.  Inclusive."}
            :project-end {:label "Project End Date"
                          :description "The date which the project finished.  Exclusive."}
            :surveyed-species {:label "Survey Species"
                               :description "A list of species included in this survey."}
            :required-fields {:label "Required Fields"
                              :description "A list of the fields required to be in the metadata."}}

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

   :settings {:preferences "Preferences"
              :survey-settings "Survey Settings"}
   :application {:dashboard "Dashboard"}
   :default-config-created "A default configuration has been created in '%s'"

   :missing  "|Missing translation: [%1$s %2$s %3$s]|"})
