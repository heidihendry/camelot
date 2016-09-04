(ns camelot.translation.en)

(def t-en
  {:problems
   {:error "[ERROR] "
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
    :config-not-found "A configuration file was not found.  Please run camelot with the 'init' option and adjust the configuration file created."}

   :survey
   {:duplicate-name "A survey with the name '%s' already exists"
    :title "Survey"
    :sidebar-title "Surveys"
    :survey-name {:label "Survey Name"
                  :description "The name which will be used to refer to the survey."}
    :survey-directory {:label "Survey Directory"
                       :description "The root directory of the survey's data"}
    :survey-sampling-point-density {:label "Sampling Point Density (metres)"
                                    :description "The distance between camera trap stations across the survey."}
    :survey-sighting-independence-threshold {:label "Sighting Independence Threshold (mins)"
                                             :description "The minimum amount of minutes which must elapse between an initial sighting, and a subsequent sighting, for the new sighting to be considered independent"}
    :survey-notes {:label "Survey Notes"
                   :description "Notes about this survey."}}

   :survey-site
   {:title "Survey Site"
    :sidebar-title "Survey Sites"
    :site-id {:label "Site"
              :description "The site to add to the survey"}}

   :trap-station-session-camera
   {:title "Session Camera"
    :sidebar-title "Session Cameras"
    :trap-station-session-camera-import-path {:label "Import Path"
                                              :description "The path to the files originally imported to this camera"}
    :trap-station-session-camera-media-unrecoverable {:label "Media Unrecoverable"
                                                      :description "The media associated with this camera session could not be recovered."}

    :camera-id {:label "Camera"
                :description "The camera to add to the Trap Station Session"}}

   :trap-station
   {:title "Trap Station"
    :sidebar-title "Trap Stations"
    :trap-station-name {:label "Trap Station Name"
                        :description "Name of this Trap Station"}
    :trap-station-longitude {:label "Longitude"
                             :description "GPS Longitude in decimal format"}
    :trap-station-latitude {:label "Latitude"
                            :description "GPS Latitude in decimal format"}
    :trap-station-altitude {:label "Altitude (meters)"
                            :description "Altitude in meters relative to sea-level"}
    :trap-station-distance-above-ground {:label "Distance above ground (meters)"
                                         :description "Distance the camera trap was positioned above ground"}
    :trap-station-distance-to-road {:label "Distance to road (meters)"
                                    :description "Distance between the camera trap and the nearest road"}
    :trap-station-distance-to-river {:label "Distance to river (meters)"
                                     :description "Distance between the camera trap and the nearest river"}
    :trap-station-distance-to-settlement {:label "Distance to settlement (meters)"
                                          :description "Distance between the camera trap and the nearest human settlement"}
    :trap-station-sublocation {:label "Sublocation"
                               :description "The name given to the location"}
    :trap-station-notes {:label "Trap Station Notes"
                         :description "Notes about this trap station"}}

   :trap-station-session
   {:title "Trap Station Session"
    :sidebar-title "Sessions"
    :trap-station-session-start-date {:label "Session Start Date"
                                      :description "The date the Trap Station started recording for this session."}
    :trap-station-session-end-date {:label "Session End Date"
                                    :description "The date the Trap Station finished recording for this session."}
    :trap-station-session-notes {:label "Session Notes"
                                 :description "Notes about this session for the trap station."}}

   :site
   {:duplicate-name "A site with the name '%s' already exists"
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
    :site-area {:label "Site Area"
                :description "Area of this site in km2."}
    :site-notes {:label "Site Notes"
                 :description "Notes about this site."}}

   :camera
   {:duplicate-name "A camera with the name '%s' already exists"
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

   :camera-status {:active "Active in field"
                   :available "Available for use"
                   :lost "Lost"
                   :stolen "Stolen"
                   :retired "Retired"}

   :checks
   {:starting "Running consistency checks..."
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
    :gps-data-missing "GPS data is missing from '%s'. For the time being, media cannot be imported without GPS longitude and latitudes set."}

   :language
   {:en "English"
    :vn "Vietnamese"}

   :metadata
   {:location.gps-longitude "GPS Longitude"
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

   :settings
   {:preferences "Preferences"
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

   :actionmenu
   {:title "Actions"}

   :action
   {:survey-sites "View Survey Sites"
    :trap-stations "View Trap Stations"
    :sightings "View Sightings"
    :maxent-report "Download MaxEnt Export"
    :effort-summary-report "Download Effort Summary"
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

   :sighting
   {:sidebar-title "Sightings"
    :title "Sighting"
    :taxonomy-id {:label "Species"
                  :description "Species in the media"}
    :sighting-quantity {:label "Quantity"
                        :description "Quantity of this species in the media."}}

   :media
   {:sidebar-title "Media"
    :title "Media"
    :media-filename {:label "Capture"
                     :description "The photo"}
    :media-capture-timestamp {:label "Capture Time"
                              :description "Capture time"}
    :media-notes {:label "Notes"
                  :description "Notes about this capture"}}

   :photo
   {:sidebar-title "Photo"
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

   :taxonomy
   {:sidebar-title "Species"
    :title "Species"
    :taxonomy-class {:label "Class"
                     :description "Class name."}
    :taxonomy-order {:label "Order"
                     :description "Order name."}
    :taxonomy-family {:label "Family"
                      :description "Family name."}
    :taxonomy-genus {:label "Genus"
                     :description "Genus name."}
    :taxonomy-species {:label "Species"
                       :description "Species name."}
    :taxonomy-common-name {:label "Common Name"
                           :description "Common name by which this taxonomy is known."}
    :species-mass-id {:label "Species mass"
                      :description "The approximate mass for an average adult."}
    :taxonomy-notes {:label "Notes"
                     :description "Notes about this species or its identification."}}

   :application
   {:import "Import"
    :library "Library"
    :organisation "Organisation"
    :surveys "Surveys"
    :analysis "Analysis"
    :sites "Sites"
    :species "Species"
    :taxonomy "Species"
    :cameras "Cameras"}
   :default-config-created "A default configuration has been created in '%s'"

   :report
   {:nights-elapsed "Nights Elapsed"
    :total-nights "Nights Elapsed"
    :independent-observations "Independent Observations"
    :independent-observations-per-night "Abundance Index"
    :time-period-start "Period Start"
    :time-period-end "Period End"
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
    :site-area "Site Area (km2)"
    :survey-site-id "Survey Site ID"
    :taxonomy-species "Species"
    :taxonomy-genus "Genus"
    :taxonomy-family "Family"
    :taxonomy-order "Order"
    :taxonomy-class "Class"
    :taxonomy-label "Species"
    :species-common-name "Common Name"
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
    :taxonomy-count "Number of Species"
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

   :words
   {:in "in"
    :done "Done"
    :or "Or"
    :na "N/A"
    :acknowledge "Acknowledge"
    :uploading "Uploading"
    :search "Search"
    :citation "Citation"
    :genus "Genus"
    :back "Back"
    :cancel "Cancel"
    :add "Add"
    :create "Create"
    :adult "Adult"
    :juvenile "Juvenile"
    :unidentified "Unidentified"
    :male "Male"
    :female "Female"
    :hide "Hide"
    :species "Species"
    :quantity "Quantity"
    :sex "Sex"
    :submit "Submit"
    :latitude "Latitude"
    :longitude "Longitude"
    :altitude "Altitude"
    :captures-lc "captures"
    :camera "Camera"
    :timestamp "Timestamp"
    :sightings "Sightings"
    :details "Details"
    :flagged-lc "flagged"
    :unflagged-lc "unflagged"
    :processed-lc "processed"
    :unprocessed-lc "unprocessed"
    :selected-lc "selected"
    :and-lc "and"
    :import "Import"
    :version "Version"
    :cameras-ls "cameras"
    :advanced "Advanced"
    :remove "Remove"
    :name "Name"
    :date "Date"
    :notes "Notes"
    :update "Update"
    :surveys "Surveys"
    :sites "Sites"
    :cameras "Cameras"
    :reports "Reports"}

   :common
   {:select "Select..."}

   :concepts
   {:lifestage "Life stage"
    :reference-quality-lc "reference quality"
    :ordinary-quality-lc "ordinary quality"
    :test-fires-lc "test-fires"
    :not-test-fires-lc "no longer test-fires"
    :trap-station "Trap Station"
    :sublocation "Sublocation"
    :state-province "State/Province"
    :site "Site"
    :site-name "Site name"
    :nearest-city "Nearest city"
    :country "Country"
    :area-covered "Area covered (km2)"
    :lifestage-abbrev "LS"
    :common-name "Common name"
    :class "Class"
    :order "Order"
    :family "Family"
    :genus "Genus"
    :species "Species"
    :species-mass "Species mass"
    :camera-name "Camera name"
    :camera-make "Camera make"
    :camera-model "Camera model"
    :camera-notes "Camera notes"}

   :camelot.component.camera.core
   {:new-camera-name-placeholder "New camera name..."
    :invalid-title "A camera with this name already exists."
    :filter-cameras "Filter cameras..."
    :blank-filter-advice "You can add cameras using the input field below"}

   :camelot.component.camera.manage
   {:validation-failure-title "Fix the errors above before submitting."
    :update-camera "Update Camera"}

   :camelot.component.library.collection
   {:upload-advice "Upload captures using 'Upload Captures' in your survey."
    :filter-notice "No matching captures found"
    :filter-advice "That's a shame. Maybe try another search?"
    :select-none-button-title "Remove all selections"
    :select-none-button "Select None"
    :select-all-button-title "Select all media on this page"
    :select-all-button "Select All"}

   :camelot.component.library.preview
   {:photo-not-selected "Photo not selected"}

   :camelot.component.library.search
   {:reference-window-button-text "Reference Window"
    :reference-window-button-title "Additional window which displays reference-quality photos of the currently selected species for identification."
    :filter-button-title "Apply the current filters"
    :filter-placeholder "Filter..."
    :filter-title "Type a keyword you want the media to contain"
    :filter-survey-title "Filter to only items in a certain survey"
    :filter-survey-all-surveys "All Surveys"
    :reference-window-partial-title "Reference photos"
    :identification-panel-button-title "Open the identification panel to apply to the selected media"
    :identification-panel-button-text "Identify Selected"
    :filter-trap-station-all-traps "All Traps"
    :flag-media-title "Flag or unflag the selected media as needing attention."
    :media-cameracheck-title "Mark the selected media as a human-caused test fire."
    :media-processed-title "Set the selected media as processed or unprocessed."
    :media-reference-quality-title "Indicates the selected media are high quality and should be used as a reference."
    :filter-unprocessed-label "Unprocessed"
    :taxonomy-add-placeholder "New species name..."
    :add-duplicate-species-error "A species with this name already exists"
    :add-new-species-label "Add a new species..."}

   :camelot.component.albums
   {:num-nights "%d nights"
    :num-photos "%d photos"
    :timestamp-information-missing "Timespan information missing"
    :import-validation-error-title "Unable to import due to validation errors."
    :import-warning-confirm "This folder may contain data with flaws. Importing it may compromise the accuracy of future analyses. Do you want to continue?"
    :no-problems "No problems found. Time to analyse!"
    :loading "Loading Data"}

   :camelot.component.deployment.core
   {:help-text "Record a camera check each time you visit a camera trap in the field. A camera trap deployment will be finished automatically once there are no 'Active' cameras assigned to it."
    :no-cameras "No Cameras Available"
    :status-question "What is the new status of this camera?"
    :replacer-question "Which camera replaced it in the field, if any?"
    :media-retrieved "Media retrieved?"
    :media-recovered "Media was recovered"
    :media-not-recovered "Media could not be recovered"
    :camera-check-date "Camera-check date"
    :date-validation-past "Date cannot be before the start date of the current session."
    :date-validation-future "Date cannot be in the future."
    :primary-camera "Primary camera"
    :secondary-camera "Secondary camera"
    :add-secondary-camera "Add a secondary camera"
    :secondary-camera-label "Secondary camera, if any"
    :validation-same-camera "Secondary camera must not be the same as the primary camera."
    :primary-camera-name "Camera name (primary)"
    :secondary-camera-name "Camera name (secondary)"
    :record-camera-check "Record camera check"
    :start-date "State date"
    :blank-item-name-lc "camera traps"
    :advice-context "These are locations where cameras are deployed in the field."
    :advice-direction "You can set some up using the button below."
    :create-title "Add a new camera trap deployment."
    :create-button "Add camera trap"}

   :camelot.component.deployment.create
   {:new-camera-name-placeholder "New camera name..."
    :camera-invalid-title "A camera with this name already exists."
    :create-new-camera "Create a new camera..."
    :new-site-name-placeholder "New site name..."
    :site-invalid-title "A site with this name already exists."
    :create-new-site "Create a new site..."
    :start-date "Start date"
    :validation-future-date "Date cannot be in the future."
    :invalid-latitude "Latitude must be in the range [-90, 90]."
    :invalid-longitude "Longitude must be in the range [-180, 180]."
    :primary-camera "Primary camera"
    :secondary-camera "Secondary camera"
    :validation-same-camera "Secondary camera must not be the same as the primary camera."
    :validation-failure "Please complete all required fields and address any errors."
    :add-camera-trap "Add Camera Trap"}

   :camelot.component.deployment.recent
   {:help-text "Drag and drop capture files on to a Camera Check to add them."
    :format-not-supported "'%s' is not in a supported format."
    :upload-error "error during upload"
    :gps-coordinates "GPS coordinates"
    :progress-bar-title "%d complete, %d failed and %d ignored."
    :show-details "Show details"
    :blank-item-name "camera checks"
    :blank-advice "These will appear when you add checks to your camera traps."}

   :camelot.component.deployment.shared
   {:sort-by "Sort by"}

   :camelot.component.error
   {:problems "There were some problems..."
    :page-not-found "A page with that name has not been sighted."
    :maybe-bug "If you think this is a bug, "
    :report-issue "Report an Issue"}

   :camelot.component.import-dialog
   {:import-from "Import from"
    :import-media "Import media"}

   :camelot.component.organisation
   {:not-implemented "Sorry, but this hasn't been developed yet."
    :organisation "Your Organisation"}

   :camelot.component.species-search
   {:scientific-name "Scientific name..."
    :search-species "Search species"}

   :camelot.component.util
   {:blank-notice-template "There aren't any %s yet"
    :use-button-below "You can set some up using the button below."
    :use-advanced-menu "You can set some up using the 'Advanced' menu below."}

   :camelot.component.report.core
   {:filter-reports-placeholder "Filter reports..."
    :item-name "reports"
    :notice "No reports matched"
    :advice "There weren't any results for this search."}

   :camelot.component.site.core
   {:new-site-name "New site name..."
    :validation-duplicate-site "A site with this name already exists."
    :item-name "sites"
    :advice "You can add sites using the input field below."
    :filter-sites "Filter sites..."}

   :camelot.component.site.manage
   {:validation-site-name "Must not be blank or have the same name as another site."
    :validation-failure "Fix the errors above before submitting."
    :default-intro "Update Site"}

   :camelot.component.species.core
   {:item-name "species"
    :manage-species "Manage species"}

   :camelot.component.species.manage
   {:search-instructions "Search and add species using the options to the right."
    :new-species-name-placeholder "New species name..."
    :validation-duplicate-species "A species with this name already exists."
    :new-or-existing "New or existing species"
    :new-species "Add a new species..."
    :expected-species "Expected species"
    :intro "Manage Species"}

   :camelot.component.species.update
   {:validation-error-title "Complete all required fields before submitting."
    :update-species "Update species"}

   :camelot.component.survey.core
   {:create-survey "Create survey"
    :manage-traps "Manage camera traps"
    :upload-captures "Upload captures"
    :species "Species"}

   :camelot.component.survey.create
   {:search-instructions "Search and add species using the options to the right."
    :survey-name "Survey name"
    :survey-name-placeholder "Survey name..."
    :survey-description "Survey description"
    :expected-species "Expected species"
    :create-survey "Create survey"
    :intro "Create Survey"
    :submit-title "Submit this survey."
    :validation-error-title "Complete all required fields before submitting."}

   :missing "[Translation missing]"})
