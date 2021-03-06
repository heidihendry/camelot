(ns camelot.translation.en)

(def t-en
  {:words
   {:acknowledge "Acknowledge"
    :add "Add"
    :advanced "Advanced"
    :and-lc "and"
    :back "Back"
    :of-lc "of"
    :on-lc "on"
    :or-lc "or"
    :cancel "Cancel"
    :continue "Continue"
    :citation "Citation"
    :close "Close"
    :confirm "Confirm"
    :create "Create"
    :next "Next"
    :date "Date"
    :delete "Delete"
    :details "Details"
    :done "Done"
    :edit "Edit"
    :hide "Hide"
    :present "Present"
    :history "History"
    :import "Import"
    :in "in"
    :name "Name"
    :no "No"
    :notes "Notes"
    :or "Or"
    :to-lc "to"
    :at-lc "at"
    :remove "Remove"
    :revert "Revert"
    :search "Search"
    :select "Select"
    :submit "Submit"
    :update "Update"
    :uploading "Uploading"
    :version "Version"
    :yes "Yes"}

   :problems
   {:root-path-missing "Please specify the absolute path to the Survey Directory in the settings panel."
    :root-path-not-found "The directory for the survey was not found or could not be read"
    :path-not-found "%s: Path not found"
    :not-directory "%s: Is not a directory"
    :species-quantity-missing "%s: has species without a quantity"
    :species-name-missing "%s: has quantity without a species"
    :config-not-found "A configuration file was not found.  Please run camelot with the 'init' option and adjust the configuration file created."}

   :survey
   {:duplicate-name "A survey with the name '%s' already exists"
    :title "Survey"
    :report-description "The survey to report on."
    :sidebar-title "Surveys"
    :survey-name.label "Survey name"
    :survey-name.description "The name that will be used to refer to the survey."
    :survey-directory.label "Survey directory"
    :survey-directory.description "The root directory of the survey's data"
    :survey-sampling-point-density.label "Sampling point density (metres)"
    :survey-sampling-point-density.description "The distance between camera trap stations across the survey."
    :survey-sighting-independence-threshold.label "Sighting independence threshold (mins)"
    :survey-sighting-independence-threshold.description "The minimum amount of minutes that must elapse between an initial sighting, and a subsequent sighting, for the new sighting to be considered independent"
    :survey-notes.label "Survey description"
    :survey-notes.description "Detail about this survey's goals and methods."}

   :survey-site
   {:title "Survey Site"
    :report-description "Data from a site within a specific survey."
    :sidebar-title "Survey Sites"
    :site-id.label "Site"
    :site-id.description "The site to add to the survey"}

   :trap-station-session-camera
   {:title "Session Camera"
    :sidebar-title "Session Cameras"
    :trap-station-session-camera-import-path.label "Import path"
    :trap-station-session-camera-import-path.description "The path to the files originally imported to this camera"
    :trap-station-session-camera-media-unrecoverable.label "Media unrecoverable"
    :trap-station-session-camera-media-unrecoverable.description "The media associated with this camera session could not be recovered."

    :camera-id.label "Camera"
    :camera-id.description "The camera to add to the Trap Station Session"
    :delete-media "Delete media"}

   :trap-station
   {:title "Trap Station"
    :sidebar-title "Trap Stations"
    :report-description "The trap station to report on"
    :trap-station-name.label "Trap station name"
    :trap-station-name.description "Name of this Trap Station"
    :trap-station-longitude.label "Longitude"
    :trap-station-longitude.description "GPS Longitude in decimal format"
    :trap-station-latitude.label "Latitude"
    :trap-station-latitude.description "GPS Latitude in decimal format"
    :trap-station-altitude.label "Altitude (meters)"
    :trap-station-altitude.description "Altitude in meters relative to sea-level"
    :trap-station-distance-above-ground.label "Distance above ground (meters)"
    :trap-station-distance-above-ground.description "Distance the camera trap was positioned above ground"
    :trap-station-distance-to-road.label "Distance to road (meters)"
    :trap-station-distance-to-road.description "Distance between the camera trap and the nearest road"
    :trap-station-distance-to-river.label "Distance to river (meters)"
    :trap-station-distance-to-river.description "Distance between the camera trap and the nearest river"
    :trap-station-distance-to-settlement.label "Distance to settlement (meters)"
    :trap-station-distance-to-settlement.description "Distance between the camera trap and the nearest human settlement"
    :trap-station-sublocation.label "Sublocation"
    :trap-station-sublocation.description "The name given to the location"
    :trap-station-notes.label "Trap station notes"
    :trap-station-notes.description "Notes about this trap station"}

   :trap-station-session
   {:title "Trap Station Session"
    :sidebar-title "Sessions"
    :trap-station-session-start-date.label "Session start date"
    :trap-station-session-start-date.description "The date the Trap Station started recording for this session."
    :trap-station-session-end-date.label "Session end date"
    :trap-station-session-end-date.description "The date the Trap Station finished recording for this session."
    :trap-station-session-notes.label "Session notes"
    :trap-station-session-notes.description "Notes about this session for the trap station."}

   :site
   {:duplicate-name "A site with the name '%s' already exists"
    :title "Site"
    :sidebar-title "Sites"
    :site-name.label "Site name"
    :site-name.description "The name that will be used to refer to the site."
    :site-country.label "Country"
    :site-country.description "The country within which this site resides."
    :site-state-province.label "State/Province"
    :site-state-province.description "The state or province within which this site resides."
    :site-city.label "City"
    :site-city.description "The city within, or nearest city to, which this site resides."
    :site-sublocation.label "Sublocation"
    :site-sublocation.description "The name of the location that this site represents."
    :site-area.label "Site area (km2)"
    :site-area.description "Area of this site in km2."
    :site-notes.label "Site notes"
    :site-notes.description "Notes about this site."}

   :camera
   {:duplicate-name "A camera with the name '%s' already exists"
    :title "Camera"
    :sidebar-title "Cameras"
    :camera-name.label "Camera name"
    :camera-name.description "The name by which this camera will be known."
    :camera-status-id.label "Status"
    :camera-status-id.description "Whether this camera is active (deployed), available for use, or retired for some reason."
    :camera-make.label "Make"
    :camera-make.description "The manufacturer or brand of the camera"
    :camera-model.label "Model"
    :camera-model.description "The model name or number of the camera"
    :camera-software-version.label "Camera Software Version"
    :camera-software-version.description "The version of the software running on the camera"
    :camera-notes.label "Notes"
    :camera-notes.description "Notes about this camera"}

   :sighting
   {:sidebar-title "Sightings"
    :title "Sighting"
    :taxonomy-id.label "Species"
    :taxonomy-id.description "Species in the media"
    :sighting-quantity.label "Quantity"
    :sighting-quantity.description "Quantity of this species in the media."}

   :media
   {:sidebar-title "Media"
    :title "Media"
    :media-filename.label "Filename"
    :media-filename.description "The photo"
    :media-capture-timestamp.label "Capture time"
    :media-capture-timestamp.description "Capture time"
    :media-notes.label "Notes"
    :media-notes.description "Notes about this photo"}

   :photo
   {:sidebar-title "Photo"
    :title "Photo Details"
    :photo-iso-setting.label "ISO setting"
    :photo-iso-setting.description "The ISO setting at the time the photo was taken."
    :photo-aperture-setting.label "Aperture"
    :photo-aperture-setting.description "The aperture of the lens at the time the photo was taken."
    :photo-exposure-value.label "Exposure value"
    :photo-exposure-value.description "The calculated exposure value of the photo."
    :photo-flash-setting.label "Flash setting"
    :photo-flash-setting.description "The flash setting at the time the photo was taken."
    :photo-fnumber-setting.label "F-Number"
    :photo-fnumber-setting.description "The f-number setting at the time the photo was taken."
    :photo-orientation.label "Orientation"
    :photo-orientation.description "The orientation of the camera when the photo was taken."
    :photo-resolution-x.label "Width (pixels)"
    :photo-resolution-x.description "The width of the photo in pixels."
    :photo-resolution-y.label "Height (pixels)"
    :photo-resolution-y.description "The height of the photo in pixels."
    :photo-focal-length.label "Focal length"
    :photo-focal-length.description "The focal length at the time the photo was taken."}

   :taxonomy
   {:sidebar-title "Species"
    :title "Species"
    :report-description "The species to report on"
    :taxonomy-class.label "Class"
    :taxonomy-class.description "Class name."
    :taxonomy-order.label "Order"
    :taxonomy-order.description "Order name."
    :taxonomy-family.label "Family"
    :taxonomy-family.description "Family name."
    :taxonomy-genus.label "Genus"
    :taxonomy-genus.description "Genus name."
    :taxonomy-species.label "Species"
    :taxonomy-species.description "Species name."
    :taxonomy-common-name.label "Common name"
    :taxonomy-common-name.description "Common name by which this taxonomy is known."
    :species-mass-id.label "Species mass"
    :species-mass-id.description "The approximate mass for an average adult."
    :taxonomy-notes.label "Notes"
    :taxonomy-notes.description "Notes about this species or its identification."}

   :camera-status
   {:active "Active in field"
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
    :surveyed-species "Species were identified in '%s' that are not known to the survey: '%s'"
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
    :survey-settings "Settings"
    :title "Settings"
    :erroneous-infrared-threshold.label "Erroneous Infrared Threshold"
    :erroneous-infrared-threshold.description "Value between 0.0 and 1.0 to set the threshold for date/time error detection"
    :infrared-iso-value-threshold.label "Infrared ISO Value Threshold"
    :infrared-iso-value-threshold.description "ISO value of the photos beyond which it is considered 'night'"
    :sighting-independence-minutes-threshold.label "Sighting Independence Threshold (mins)"
    :sighting-independence-minutes-threshold.description "The minimum amount of minutes that must elapse between an initial sighting, and a subsequent sighting, for the new sighting to be considered independent"
    :send-usage-data.label "Send anonymous usage data"
    :send-usage-data.description "If this is enabled, anonymous usage data will be sent to the Camelot Project.  This data will only be used for the purpose of improving Camelot."
    :language.label "Language"
    :language.description "Interface language to use"
    :root-path.label "Survey Directory"
    :root-path.description "Pathname to the root directory of this survey's photo data. This must be as it would appear to the system running the Camelot server process."
    :night-start-hour.label "Night Start Time"
    :night-start-hour.description "Hour beyond which it is considered night"
    :night-end-hour.label "Night End Time"
    :night-end-hour.description "Hour at which it is considered daylight"
    :project-start.label "Project Start Date"
    :project-start.description "The date that the project commenced.  Inclusive."
    :project-end.label "Project End Date"
    :project-end.description "The date that the project finished.  Exclusive."
    :surveyed-species.label "Survey Species"
    :surveyed-species.description "A list of species included in this survey."
    :required-fields.label "Required Fields"
    :required-fields.description "A list of the fields required to be in the metadata."
    :species-name-style-scientific "Scientific name"
    :species-name-style-common "Common name"
    :species-name-style.label "Species name style"
    :species-name-style.description "Style of name to show when identifying species"}

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

   :application
   {:import "Import"
    :library "Library"
    :organisation "Organisation"
    :about "About"
    :detector "Detection"
    :surveys "Surveys"
    :analysis "Analysis"
    :sites "Sites"
    :species "Species"
    :taxonomy "Species"
    :cameras "Cameras"}
   :default-config-created "A default configuration has been created in '%s'"

   :report
   {:absolute-path "Absolute path to image file"
    :nights-elapsed "Nights Elapsed"
    :total-nights "Nights Elapsed"
    :independent-observations "Independent Observations"
    :independent-observations-per-night "Abundance Index"
    :media-capture-date "Capture Date"
    :media-capture-time "Capture Time"
    :sighting-time-delta-seconds "Time From Last Sighting (seconds)"
    :sighting-time-delta-minutes "Time From Last Sighting (minutes)"
    :sighting-time-delta-hours "Time From Last Sighting (hours)"
    :sighting-time-delta-days "Time From Last Sighting (days)"
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
    :taxonomy-common-name "Common Name"
    :taxonomy-notes "Species Notes"
    :taxonomy-id "Species ID"
    :taxonomy-updated "Taxonomy updated"
    :taxonomy-created "Taxonomy created"
    :species-mass-id "Species Mass ID"
    :species-mass-start "Species Mass Start"
    :species-mass-end "Species Mass End"
    :camera-id "Camera ID"
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
    :trap-station-session-camera-media-unrecoverable "Trap Station Session Camera Media Unrecoverable"
    :media-id "Media ID"
    :site-count "Number of Sites"
    :trap-station-session-camera-count "Number of Cameras in Sessions"
    :trap-station-session-count "Number of Trap Station Sessions"
    :trap-station-count "Number of Trap Stations"
    :media-count "Number of Photos"
    :species-name "Species Name"
    :taxonomy-count "Number of Species"
    :media-capture-timestamp "Media Capture Timestamp"
    :media-notes "Media Notes"
    :media-filename "Media Filename"
    :media-format "Media Format"
    :media-attention-needed "Media Attention Needed"
    :media-cameracheck "Media Camera Check"
    :media-processed "Media Processed"
    :media-created "Media created"
    :media-updated "Media updated"
    :media-uri "Media URI"
    :sighting-id "Sighting ID"
    :sighting-created "Sighting Created"
    :sighting-updated "Sighting Updated"
    :sighting-quantity "Sighting Quantity"
    :photo-iso-setting "Photo ISO Setting"
    :photo-exposure-value "Photo Exposure Value"
    :photo-flash-setting "Photo Flash Setting"
    :photo-focal-length "Photo Focal length"
    :photo-fnumber-setting "Photo F-Number Setting"
    :photo-orientation "Photo Orientation"
    :photo-resolution-x "Photo X Resolution (pixels)"
    :photo-resolution-y "Photo Y Resolution (pixels)"}

   :camelot.report.module.builtin.reports.camera-traps
   {:title "[CamtrapR] Camera Trap Export"
    :description "A CamtrapR-compatible export of camera trap details. Set 'byCamera' to TRUE when importing into CamtrapR."}

   :camelot.report.module.builtin.reports.effort-summary
   {:title "Effort Summary"
    :description "A breakdown of sites in a survey and their trap stations."}

   :camelot.report.module.builtin.reports.full-export
   {:title "Full Export"
    :description "Export of data in Camelot, with one row per unique record."}

   :camelot.report.module.builtin.reports.survey-export
   {:title "Survey Export"
    :description "Export of data from a survey in Camelot, with one row per unique record."}

   :camelot.report.module.builtin.reports.raw-data-export
   {:title "Raw Data Export"
    :description "Details about each uploaded capture."}

   :camelot.report.module.builtin.reports.species-statistics
   {:title "Species Statistics"
    :description "Sightings breakdown for a single species across all surveys."}

   :camelot.report.module.builtin.reports.summary-statistics
   {:title "Summary Statistics"
    :description "Summary report for the observations of each species in a survey."}

   :camelot.report.module.builtin.reports.survey-site
   {:title "Survey Site Statistics"
    :description "The observations of each species in a Survey Site."}

   :camelot.report.module.builtin.reports.trap-station
   {:title "Trap Station Statistics"
    :description "Observations at a given trap station and the time elapsed gathering those observations."}

   :camelot.report.module.builtin.reports.record-table
   {:title "[CamtrapR] Record Table"
    :description "A CamtrapR-compatible RecordTable export of independent sightings."
    :media-directory "Media Directory"}

   :camelot.report.module.builtin.reports.occupancy-matrix
   {:title-count "Occupancy Matrix - Species Count"
    :description-count "Occupancy matrix showing the number of independent sightings of a species per day per trap station. Suitable for use with PRESENCE."
    :title-presence "Occupancy Matrix - Species Presence"
    :description-presence "Occupancy matrix showing whether a species was present at a trap station on a certain day. Suitable for use with PRESENCE."
    :start-date "Start date"
    :start-date-description "Date to begin the matrix from, inclusive."
    :end-date "End date"
    :end-date-description "Date to end the matrix on, inclusive."}

   :camelot.import.core
   {:timestamp-outside-range "Timestamp is outside of the session dates."}

   :camelot.component.camera.core
   {:new-camera-name-placeholder "New camera name..."
    :invalid-title "A camera name was not provided or a camera with this name already exists."
    :filter-cameras "Filter cameras..."
    :confirm-delete "Are you sure you want to delete this camera? This will also delete any associated media."
    :blank-filter-advice "You can add cameras using the input field below"
    :blank-item-name "cameras"}

   :camelot.component.camera.manage
   {:validation-failure-title "Fix the errors above before submitting."
    :update-camera "Update camera"
    :trap-station-column "Trap station"
    :trap-station-session-start-date-column "Session start date"
    :trap-station-session-end-date-column "Session end date"}

   :camelot.component.library.core
   {:delete-media-question "Do you really want to delete all selected media?"
    :delete-media-title "Confirm Delete"
    :delete-sightings-question "Do you really want to delete all sightings from all selected media?"
    :delete-sightings-title "Confirm Delete"
    :media-select-count "There are %d items in the current selection that will be deleted."
    :sighting-select-count "There are %d sightings in the current selection that will be deleted."}

   :camelot.component.library.collection
   {:upload-advice "Add media by adding Camera Trap Stations, adding Camera Checks to those, and then Upload Media in your survey."
    :filter-notice "No matching media found"
    :filter-advice "That's a shame. Maybe try another search?"
    :select-none-button-title "Remove all selections"
    :select-none-button "Select None"
    :select-all-button-title "Select all media on this page"
    :select-all-button "Select All"
    :item-name "media"
    :identify-selected "Identify selected"
    :reference-window-no-media-notice "No reference quality images found"
    :reference-window-no-media-advice "Images that will be useful for identifying a species in future can be marked as 'reference quality' in the main window, and will show up here in future."
    :selected-media-from-multiple-surveys "Selected media must belong to only one survey in order to be identified."}

   :camelot.component.library.identify
   {:identify-selected "Identify Selected"
    :taxonomy-add-placeholder "New species name..."
    :species-format-error "Invalid scientific name for species"
    :add-new-species-label "Add a new species..."
    :submit-not-allowed "One or more fields cannot be submitted in their current state."}

   :camelot.component.library.util
   {:reference-quality "reference quality"
    :ordinary-quality "ordinary quality"
    :identified "identified"
    :test-fires "test-fires"
    :not-test-fires "no longer test-fires"
    :flagged "flagged"
    :unflagged "unflagged"
    :processed "processed"
    :unprocessed "unprocessed"
    :selected "selected"}

   :camelot.component.library.preview
   {:photo-not-selected "Photo not selected"
    :species-not-in-survey "[Species not in this survey]"
    :brightness-label "Brightness"
    :contrast-label "Contrast"
    :sightings "Sightings"
    :delete-media "Delete selected media"
    :delete-sightings "Delete sightings from selected media"}

   :camelot.component.library.search
   {:reference-window-button-text "Reference window"
    :reference-window-button-title "Additional window that displays reference-quality photos of the currently selected species for identification."
    :filter-button-title "Apply the current filters"
    :filter-placeholder "Search..."
    :filter-title "Type a keyword you want the media to contain"
    :filter-survey-title "Filter to only items in a certain survey"
    :filter-survey-all-surveys "All Surveys"
    :reference-window-partial-title "Reference photos"
    :identification-panel-button-title "Open the identification panel to apply to the selected media"
    :identification-panel-button-text "Identify selected"
    :filter-trap-station-all-traps "All Traps"
    :flag-media-title "Flag or unflag the selected media as needing attention."
    :media-cameracheck-title "Mark the selected media as a human-caused test fire."
    :media-processed-title "Set the selected media as processed or unprocessed."
    :media-reference-quality-title "Indicates the selected media are high quality and should be used as a reference."
    :filter-unprocessed-label "Unprocessed"
    :filter-animals-label "Has animal?"}

   :camelot.component.albums
   {:num-nights "%d nights"
    :num-photos "%d photos"
    :timestamp-information-missing "Timespan information missing"
    :import-validation-error-title "Unable to import due to validation errors."
    :import-warning-confirm "This folder may contain data with flaws. Importing it may compromise the accuracy of future analyses. Do you want to continue?"
    :no-problems "No problems found. Time to analyse!"
    :loading "Loading Data"}

   :camelot.component.deployment.camera-check
   {:help-text "Record a camera check each time you visit a camera trap in the field. A camera trap deployment will be finished automatically once there are no 'Active' cameras assigned to it."
    :no-cameras "No Cameras Available"
    :no-replacement-camera "No replacement camera"
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
    :finalised-trap-notice "This camera trap has been finalised."
    :finalised-trap-advice "Camelot will automatically finalise a camera trap station once it no longer has any active cameras. Create a new camera trap station to continue trapping at this location."}

   :camelot.component.deployment.core
   {:primary-camera-name "Camera name (primary)"
    :secondary-camera-name "Camera name (secondary)"
    :record-camera-check "Record camera check"
    :start-date "Session start date"
    :end-date "Session end date"
    :blank-item-name-lc "camera trap stations"
    :advice-context "These are locations where cameras are deployed in the field."
    :advice-direction "You can set some up using the button below."
    :create-title "Add a new camera trap deployment."
    :create-button "Add camera trap"
    :confirm-delete "Are you sure you want to delete this camera trap station, including any media and camera checks associated with it?"
    :finalised "Finalised"}

   :camelot.component.deployment.create
   {:new-camera-name-placeholder "New camera name..."
    :camera-invalid-title "A camera with this name already exists."
    :create-new-camera "Create a new camera..."
    :new-site-name-placeholder "New site name..."
    :site-invalid-title "A site name was not provided or site with this name already exists."
    :create-new-site "Create a new site..."
    :start-date "Start date"
    :validation-future-date "Date cannot be in the future."
    :invalid-latitude "Latitude must be in the range [-90, 90]."
    :invalid-longitude "Longitude must be in the range [-180, 180]."
    :primary-camera "Primary camera"
    :secondary-camera "Secondary camera"
    :validation-same-camera "Secondary camera must not be the same as the primary camera."
    :validation-failure "Please complete all required fields and address any errors."
    :add-camera-trap "Add Camera Trap Station"
    :edit-camera-trap "Edit Camera Trap Station"}

   :camelot.component.progress-bar
   {:progress-bar-title "%d complete, %d failed and %d ignored."}

   :camelot.component.deployment.recent
   {:help-text "Drag and drop media files on to a Camera Check to add them."
    :format-not-supported "'%s' is not in a supported format."
    :upload-error "error during upload"
    :gps-coordinates "GPS coordinates"
    :show-details "Show details"
    :blank-item-name "camera checks"
    :media-uploaded "Media uploaded"
    :confirm-delete "Are you sure you want to delete this camera check?"
    :confirm-delete-has-media "Are you sure you want to delete this camera check? This will also delete any associated media."
    :blank-advice "These will appear when you add checks to your camera trap stations."}

   :camelot.component.deployment.shared
   {:sort-by "Sort by"
    :start-date "Start date"
    :end-date "End date"}

   :camelot.component.notification
   {:problems "There were some problems..."
    :generic-error "The operation could not be completed. This could be because the input is not valid. The details of the error are below."
    :page-not-found "A page with that name has not been sighted."
    :maybe-bug "If you think this is a bug, "
    :report-issue "report an issue"
    :ask-on-forum "ask on the forum"}

   :camelot.component.import-dialog
   {:import-from "Import from"
    :import-media "Import media"}

   :camelot.component.organisation
   {:not-implemented "Sorry, but this hasn't been developed yet."
    :organisation "Your Organisation"
    :cameras "Cameras"
    :surveys "Surveys"
    :sites "Sites"
    :reports "Reports"
    :backup "Backup / restore"}

   :camelot.component.species-search
   {:scientific-name "Scientific name..."
    :search-species "Search species by scientific name"}

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
    :confirm-delete "Are you sure you want to delete this site? This will also delete any associated media."
    :item-name "sites"
    :advice "You can add sites using the input field below."
    :filter-sites "Filter sites..."}

   :camelot.component.site.manage
   {:validation-site-name "Must not be blank or have the same name as another site."
    :validation-failure "Fix the errors above before submitting."
    :default-intro "Update Site"}

   :camelot.component.species.core
   {:item-name "species"
    :manage-species "Manage species"
    :confirm-delete "Are you sure you want to remove this species from the survey?"}

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
    :manage-traps "Manage camera trap stations"
    :upload-captures "Upload media"
    :species "Species"
    :files "Related files"
    :import "Bulk import"
    :settings "Settings"}

   :camelot.component.survey.settings
   {:details "Survey details"
    :sighting-fields "Sighting fields"
    :delete-survey "Delete survey"
    :confirm-delete "Are you sure you want to delete this survey? This will also delete any associated media and files."
    :survey-name-placeholder "Survey name..."
    :validation-error-title "Ensure all fields have valid content before updating."
    :validation-survey-name-blank "Survey name must not be blank"
    :validation-survey-name-duplicate "Survey cannot have the same name as another survey"
    :validation-survey-notes-blank "Survey description must not be blank"
    :validation-survey-sighting-independence-threshold-not-a-number "Independence threshold must be a number"}

   :sighting-field
   {:sighting-field-label.label "Sighting field label"
    :sighting-field-label.description "Label of the sighting field"
    :sighting-field-key.label "Sighting field key"
    :sighting-field-key.description "Key that identifies the sighting field. May use the same key across surveys to associate fields."
    :sighting-field-datatype.label "Sighting field type"
    :sighting-field-datatype.description "The type of input field to create. This selection influences how the field behaves."
    :sighting-field-required.label "Sighting field required?"
    :sighting-field-required.description "Whether the field must have a value before a sighting can be submitted."
    :sighting-field-default.label "Sighting field default"
    :sighting-field-default.description "The default value to use for a new sighting."
    :sighting-field-affects-independence.label "Sighting field affects independence?"
    :sighting-field-affects-independence.description "In some reports, Camelot filters sightings that are considered 'dependent'. Consult Camelot's documentation for more details sighting independence."
    :sighting-field-ordering.label "Sighting field ordering"
    :sighting-field-ordering.description "The placement of this field in the form, relative to other fields."}

   :camelot.component.survey.sighting-fields
   {:page-title "Sighting fields"
    :new-field "New field"
    :new-field-from-template "New field from template"
    :sighting-field-label.label "Label"
    :sighting-field-label.description "Label of the sighting field"
    :sighting-field-key.label "Identification key"
    :sighting-field-key.description "Key that identifies the sighting field. May use the same key across surveys to associate fields."
    :sighting-field-datatype.label "Field type"
    :sighting-field-datatype.description "The type of input field to create. This selection influences how the field behaves."
    :sighting-field-options.label "Select options"
    :sighting-field-required.label "Required?"
    :sighting-field-required.description "Whether the field must have a value before a sighting can be submitted."
    :sighting-field-default.label "Default value"
    :sighting-field-default.description "The default value to use for a new sighting."
    :sighting-field-affects-independence.label "Affects independence?"
    :sighting-field-affects-independence.description "In some reports, Camelot filters sightings that are considered 'dependent'. Consult Camelot's documentation for more details sighting independence."
    :sighting-field-ordering.label "Ordering"
    :sighting-field-ordering.description "The placement of this field in the form, relative to other fields."
    :delete-question "Are you sure you want to delete this sighting field? This will also remove any sighting data associated with this field."
    :add-option "Add option"
    :template "Select template"
    :default-template "Default"
    :help-text "Sighting fields are used to capture more information during identification."}

   :camelot.component.bulk-import.core
   {:download "Create CSV"
    :title "Bulk Import"
    :survey-menu "Survey menu"
    :ready-to-upload "Import from CSV"
    :path-name-placeholder "Absolute or relative path to a folder..."
    :help-text-step-1 "Step 1. Automatically create a CSV of all data scanned in your survey folder."
    :help-text-step-2 "Step 2. Modify the CSV as you see fit, then start the import once you're ready."
    :survey-directory "Path to survey directory"}

   :camelot.import.validate
   {:camera-overlap "%s is used in multiple sessions between %s."
    :filesystem-space "Import requires %d %s of disk space, but only %d %s is available."
    :session-dates "Media not within session dates. CSV row: %d."
    :check-sighting-assignment "A sighting should be created, but not all fields have a value on. CSV row: %d."
    :future-timestamp "Session end date is in the future.  CSV row: %d."
    :session-start-before-end "Session end date is before the session start date. CSV row: %d."}

   :camelot.component.bulk-import.mapper
   {:title "Bulk Import"
    :scanning "Preparing data for mapping..."
    :import-status-dialog-title "Importing..."
    :validation-mismatch "One or more fields do not have the necessary data to satisfy the assigned mappings."
    :import-started "Bulk import has started. You can continue using Camelot while the import is underway, and view its status at any time using the menu item near the top right-hand corner, as pictured below."
    :progress-bar-label "Import progress"
    :import-failed "Bulk import failed to start. This might be a bug. Technical details of the error follow:"
    :validation-problem "Some inconsistencies have been found with data to be imported. As a result the import has been aborted. These inconsistencies are detailed below."
    :sample-ui "An example image of the UI."
    :initialising "Preparing to start the bulk import. Please wait."
    :please-wait "Please wait for the import to commence."
    :status-code "Status code"
    :validation-missing "One or more required fields do not have a mapping assigned."
    :sightings-partially-mapped "One or more sighting fields are set, but a selection must be made for either all or none. The sighting fields are: %s."
    :validation-passed "Submit mappings for import."
    :invalid-csv "File could not be uploaded. Please check it is a valid CSV and try again."}

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

   :camelot.component.survey.file
   {:upload-time "Upload time"
    :upload-file "Upload file"
    :file-size "File size"
    :item-name "files"
    :advice "You can upload some below, if you'd like."
    :confirm-delete "Are you sure you want to delete this file?"}

   :camelot.util.model
   {:schema-not-found "No schema could be found for the column '%s'."
    :datatype-and-required-constraint-problem "All records for this field should be %s and a value should always be present, but this is not the case."
    :datatype-problem-only "All records for this field should be %s, but this is not the case."
    :required-constraint-problem-only "A value should always be present, but this is not the case."
    :max-length-problem "Field requires a maximum input length of %d but maximum length of the input is %d."
    :calculated-schema-not-available "Could not determine the metadata about column '%s'."
    :datatype-integer "an integer"
    :datatype-number "a number"
    :datatype-timestamp "a timestamp"
    :datatype-longitude "a value in the range [-180, 180]"
    :datatype-latitude "a value in the range [-90, 90]"
    :datatype-boolean "either 'true' or 'false'"
    :datatype-file "the path to a file"
    :datatype-string "a string"
    :datatype-date "a date"}

   :datatype
   {:text "Text input"
    :textarea "Multi-line text input"
    :number "Number"
    :select "Drop down"
    :checkbox "Checkbox"}

   :camelot.component.nav
   {:bulk-import-progress-label "Progress of bulk import"
    :bulk-import-status "Details about a runing bulk import."
    :bulk-import-failures "Some records have failed to import. Click for more information."
    :bulk-import-success "All media imported successfully."
    :bulk-import-complete "Import complete"
    :bulk-import-complete-with-errors "Import complete (with errors)"
    :bulk-import-cancelled "Import cancelled"
    :bulk-import-calculating "Calculating time remaining..."
    :bulk-import-time-remaining "Estimated time remaining"
    :bulk-import-failed-paths "The following files failed to import"}

   :camelot.model.trap-station-session
   {:trap-station-session-closed-label "%s to %s"
    :trap-station-session-ongoing-label "%s and ongoing"}

   :camelot.import.template
   {:directory-not-found "%s: Directory not found"
    :directory-not-readable "%s: directory not readable"
    :no-media-found "%s: no media found"}

   :camelot.component.about
   {:title "About"}

   :camelot.validation.validated-component
   {:not-empty "Must not be empty"
    :not-keyword "May contain only lower-case alphanumeric characters and hyphen ('-')"
    :too-long "Must not be longer than %d characters"
    :not-distinct "Must not already be in use"}

   :missing "[Translation missing]"})
