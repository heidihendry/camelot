(ns camelot.translation.en)

(def t-en
  {:problems {:error "[ERROR] "
              :warn "[WARNING] "
              :info "[INFO] "
              :ignore "[IGNORE] "
              :okay "[OK] "
              :root-path-missing "Please specify the absolute path to the Survey Directory in the settings panel."
              :root-path-not-found "The directory for the survey was not found or could not be read"
              :path-not-found "%s: Path not found"
              :not-directory "%s: Is not a directory"
              :read-permission-denied "%s: Could not be read: permission denied"
              :species-quantity-missing "%s: has species without a quantity"
              :species-name-missing "%s: has quantity without a species"
              :default-config-exists "A default configuration file already exists. Please delete '%s' and try again."
              :config-not-found "A configuration file was not found.  Please run camelot with the 'init' option and adjust the configuration file created."
              }

   :survey {:duplicate-name "A survey with the name '%s' already exists"
            :title "Survey"
            :sidebar-title "Surveys"
            :survey-name {:label "Survey Name"
                          :description "The name which will be used to refer to the survey."}
            :survey-directory {:label "Survey Directory"
                               :description "The root directory of the survey's data"}
            :survey-sampling-point-density {:label "Sampling Point Density (metres)"
                                            :description "The distance between camera trap stations across the survey."}
            :survey-notes {:label "Survey Notes"
                           :description "Notes about this survey."}}

   :survey-site {:title "Survey Site"
                 :sidebar-title "Survey Sites"
                 :site-id {:label "Site"
                           :description "The site to add to the survey"}}

   :trap-station-session-camera {:title "Session Camera"
                                 :sidebar-title "Session Cameras"
                                 :trap-station-session-camera-import-path {:label "Import Path"
                                                                           :description "The path to the files originally imported to this camera"}
                                 :camera-id {:label "Camera"
                                             :description "The camera to add to the Trap Station Session"}}

   :trap-station {:title "Trap Station"
                  :sidebar-title "Trap Stations"
                  :trap-station-name {:label "Trap Station Name"
                                      :description "Name of this Trap Station"}
                  :trap-station-longitude {:label "Longitude"
                                           :description "GPS Longitude in decimal format"}
                  :trap-station-latitude {:label "Latitude"
                                          :description "GPS Latitude in decimal format"}
                  :trap-station-altitude {:label "Altitude (meters)"
                                          :description "Altitude in meters relative to sea-level"}
                  :trap-station-sublocation {:label "Sublocation"
                                             :description "The name given to the location"}
                  :trap-station-notes {:label "Trap Station Notes"
                                       :description "Notes about this trap station"}}

   :trap-station-session {:title "Trap Station Session"
                          :sidebar-title "Sessions"
                          :trap-station-session-start-date {:label "Session Start Date"
                                                            :description "The date the Trap Station started recording for this session."}
                          :trap-station-session-end-date {:label "Session End Date"
                                                          :description "The date the Trap Station finished recording for this session."}
                          :trap-station-session-notes {:label "Session Notes"
                                                       :description "Notes about this session for the trap station."}}

   :site {:duplicate-name "A site with the name '%s' already exists"
          :title "Site"
          :sidebar-title "Sites"
          :site-name {:label "Site Name"
                      :description "The name which will be used to refer to the site."}
          :site-country {:label "Country"
                         :description "The country within which this site resides."}
          :site-state-province {:label "State/Province"
                                :description "The state or province within which this site resides."}
          :site-city {:label "City"
                      :description "The city within which, or nearest city to which, this site resides."}
          :site-sublocation {:label "Sublocation"
                             :description "The name of the location which this site represents."}
          :site-notes {:label "Site Notes"
                       :description "Notes about this site."}}

   :camera {:duplicate-name "A camera with the name '%s' already exists"
            :title "Camera"
            :sidebar-title "Cameras"
            :camera-name {:label "Camera Name"
                          :description "The name by which this camera will be known."}
            :camera-status-id {:label "Status"
                               :description "Whether this camera is active (deployed), available for use, or retired for some reason."}
            :camera-make {:label "Make"
                          :description "The manufacturer or brand of the camera"}
            :camera-model {:label "Model"
                           :description "The model name or number of the camera"}
            :camera-software-version {:label "Camera Software Version"
                                      :description "The version of the software running on the camera"}
            :camera-notes {:label "Notes"
                           :description "Notes about this camera"}}

   :camera-status {:active "Active"
                   :available "Available"
                   :lost "Lost"
                   :stolen "Stolen"
                   :retired "Retired"}

   :checks {:starting "Running consistency checks..."
            :failure-notice "FAIL: %s:"
            :problem-without-reason "%s. The exact reason is not known."
            :stddev-before "The timestamp on '%s' is significantly before other timestamps"
            :stddev-after "The timestamp on '%s' is significantly after other timestamps"
            :project-date-before "The timestamp on '%s' is before the project start date"
            :project-date-after "The timestamp on '%s' is before the project start date"
            :time-light-sanity "The photo exposure values indicate that the timestamp may be wrong on numerous photos"
            :camera-checks "The photos do not have 2 or more photos indicated as being camera check points"
            :headline-consistency "The headline of the photos differs across photos. (Tested '%s' and '%s')"
            :source-consistency "The phase of the photos differs across photos. (Tested '%s' and '%s')"
            :camera-consistency "The camera used to take the photos differs. (Tested '%s' and '%s')"
            :required-fields "Required fields are missing from '%s': %s"
            :album-has-data "The dataset for a period must not be empty"
            :sighting-consistency-quantity-needed "Found a species sighting in '%s', but a quantity was not found"
            :sighting-consistency-species-needed "Found a sighting quantity in '%s', but a species was not found"
            :surveyed-species "Species were identified in '%s' which are not known to the survey: '%s'"
            :future-timestamp "The timestamp on '%s' is in the future"
            :invalid-photos "A file is missing one or more essential fields: '%s'"
            :inconsistent-timeshift "The timeshifts from the original date/time are not consistent in this folder. (Tested '%s' and '%s')"
            :gps-data-missing "GPS data is missing from '%s'. For the time being, media cannot be imported without GPS longitude and latitudes set."
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

   :settings {:preferences "Preferences"
              :survey-settings "Survey Settings"
              :title "Settings"
              :erroneous-infrared-threshold {:label "Erroneous Infrared Threshold"
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

   :actionmenu {:title "Actions"}
   :action {:survey-sites "View Survey Sites"
            :trap-stations "View Trap Stations"
            :sightings "View Sightings"
            :maxent-report "Download MaxEnt Export"
            :summary-statistics-report "Download Summary Statistics"
            :species-statistics-report "Download Species Statistics"
            :raw-data-export "Export Raw Data"
            :trap-station-report "Download Camera Trap Statistics"
            :survey-site-report "Download Survey Site Statistics"
            :media "View Media"
            :sessions "View Sessions"
            :photo "View Photo Metadata"
            :trap-station-session-cameras "View Cameras"
            :edit "Edit"
            :delete "Delete"}
   :sighting {:sidebar-title "Sightings"
              :title "Sighting"
              :species-id {:label "Species"
                           :description "Species in the media"}
              :sighting-quantity {:label "Quantity"
                                  :description "Quantity of this species in the media."}}
   :media {:sidebar-title "Media"
           :title "Media"
           :media-filename {:label "Capture"
                            :description "The photo"}
           :media-capture-timestamp {:label "Capture Time"
                                     :description "Capture time"}
           :media-notes {:label "Notes"
                         :description "Notes about this capture"}}
   :photo {:sidebar-title "Photo"
           :title "Photo Details"
           :photo-iso-setting {:label "ISO setting"
                               :description "The ISO setting at the time the photo was taken."}
           :photo-aperture-setting {:label "Aperture"
                                    :description "The aperture of the lens at the time the photo was taken."}
           :photo-exposure-value {:label "Exposure value"
                                  :description "The calculated exposure value of the photo."}
           :photo-flash-setting {:label "Flash setting"
                                 :description "The flash setting at the time the photo was taken."}
           :photo-fnumber-setting {:label "F-Number"
                                   :description "The f-number setting at the time the photo was taken."}
           :photo-orientation {:label "Orientation"
                               :description "The orientation of the camera when the photo was taken."}
           :photo-resolution-x {:label "Width (pixels)"
                                :description "The width of the photo in pixels."}
           :photo-resolution-y {:label "Height (pixels)"
                                :description "The height of the photo in pixels."}
           :photo-focal-length {:label "Focal length"
                                :description "The focal length at the time the photo was taken."}}
   :species {:sidebar-title "Species"
             :title "Species"
             :species-scientific-name {:label "Scientific Name"
                                       :description "Scientific name by which this species is known."}
             :species-common-name {:label "Common Name"
                                   :description "Common name by which this species is known."}
             :species-notes {:label "Notes"
                             :description "Notes about this species or its identification."}}
   :application {:import "Import"
                 :library "Library"
                 :surveys "Surveys"
                 :analysis "Analysis"
                 :sites "Sites"
                 :species "Species"
                 :cameras "Cameras"}
   :default-config-created "A default configuration has been created in '%s'"

   :report {:nights-elapsed "Nights Elapsed"
            :independent-observations "Independent Observations"
            :independent-observations-per-night "Abundance Index"
            :percent-nocturnal "Nocturnal (%%)"
            :presence-absence "Presence"
            :survey-id "Survey ID"
            :survey-name "Survey Name"
            :survey-directory "Survey Directory"
            :survey-notes "Survey Notes"
            :site-id "Site ID"
            :site-name "Site Name"
            :site-sublocation "Site Sublocation"
            :site-city "Site City"
            :site-state-province "Site State Province"
            :site-country "Site Country"
            :site-notes "Site Notes"
            :survey-site-id "Survey Site ID"
            :species-scientific-name "Species Scientific Name"
            :species-common-name "Species Common Name"
            :species-notes "Species Notes"
            :species-id "Species ID"
            :camera-id "Camera Id"
            :camera-name "Camera Name"
            :camera-make "Camera Make"
            :camera-model "Camera Model"
            :camera-notes "Camera Notes"
            :trap-station-id "Trap Station ID"
            :trap-station-name "Trap Station Name"
            :trap-station-longitude "Trap Station Longitude"
            :trap-station-latitude "Trap Station Latitude"
            :trap-station-altitude "Trap Station Altitude"
            :trap-station-notes "Trap Station Notes"
            :trap-station-session-start-date "Trap Station Session Start Date"
            :trap-station-session-end-date "Trap Station Session End Date"
            :trap-station-session-id "Trap Station Session ID"
            :trap-station-session-camera-id "Trap Station Session Camera ID"
            :media-id "Media ID"
            :site-count "Number of Sites"
            :trap-station-session-camera-count "Number of Cameras in Sessions"
            :trap-station-session-count "Number of Trap Station Sessions"
            :trap-station-count "Number of Trap Stations"
            :media-count "Number of Photos"
            :media-capture-timestamp "Media Capture Timestamp"
            :media-notes "Media Notes"
            :media-filename "Media Filename"
            :sighting-quantity "Sighting Quantity"
            :photo-iso-setting "Photo ISO Setting"
            :photo-exposure-value "Photo Exposure Value"
            :photo-flash-setting "Photo Flash Setting"
            :photo-fnumber-setting "Photo F-Number Setting"
            :photo-orientation "Photo Orientation"
            :photo-resolution-x "Photo X Resolution (pixels)"
            :photo-resolution-y "Photo Y Resolution (pixels)"}
   :missing  "|Missing translation: [%1$s %2$s %3$s]|"})
