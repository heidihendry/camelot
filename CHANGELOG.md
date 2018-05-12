## ChangeLog
### 1.4.0
#### Enhancements
* Migrate sighting 'sex' and 'lifestage' fields to Sighting Fields.
* Significantly improve search performance in the library
* Create a backup of the database automatically before upgrading
* Provide tailored database configuration for improved performance
* Add negation to library search expressions (e.g., "flagged:true !species:*")
* Add detection for whether bulk-import can be used for a survey instead of prompting
* Improve the way error messages are displayed

#### Notes
This release moves 'sex' and 'lifestage' fields to the Sighting Fields
functionality. This opens up a bunch of flexibility around these fields.

For this upgrade Camelot reserves use of the sighting field keys "sex" and
"lifestage". The upgrade to 1.4.0 may fail should sighting fields with these
keys exist.  If this is the case, the keys of the existing fields should be
changed using an older version of Camelot. (i.e., 1.3.x)

### 1.3.5
#### Enhancements
* Set camera status back to 'Available' when an active session it is used in is deleted

#### Fixes
* Fix missing form validation around sites and cameras when creating trap stations
* Fix date format in Raw data export

### 1.3.4
#### Enhancements
* Allow sightings to be edited

#### Fixes
* Fix an error on creating camera trap stations on systems with a localisation preference of "," as the decimal point.

### 1.3.3
#### Fixes
* Fix opening of the settings sidepanel

### 1.3.2
#### Enhancements
* Allow effort summary to be created across all surveys
* Add columns for reporting context to effort summary, survey site, trap station and summary statistics reports
* Report columns 'total nights' & 'nights elapsed' now exclude sessions where no media was recoverable
* Add validation for partial sighting configuration during bulk import

#### Fixes
* Fix lack of detail for options for survey site report
* Fix period start/end dates in effort estimate report

### 1.3.1
#### Fixes
* Fix silent failure to upload a non-valid CSV file for bulk import
* Fix detection of CSV files for bulk import for some operating systems
* Fix transient error which could arise when navigating to the library
* Fix link to issues page when error occurs
* Fix bug in bulk import where trap stations are not reused when GPS coords are very precise

### 1.3.0
#### Enhancements
* Adds user-definable fields for capturing additional sighting data ("sighting fields")
* Adds keybinding to delete media currently selected in the library
* Adds ability to change survey details via the main UI
* Adds per-survey configuration of independence threshold
* Adds 'back' button to survey menu and report creation
* Improves clarity of thumbnails while hoving in the library
* It is now possible to select a range of images in the library by dragging

#### Fixes
* Fixes a bug which would cause zero results to be found when searching on some fields
* Fixes automatic opening of Camelot in a new browser tab on startup
* Fixes selection of thumbnails in the library when the underlying image is not found
* Fixes a bug where search terms would be ignored when applying other filters afterwards

#### Internal
* Switch build tooling to boot (http://boot-clj.com/)
* Adds a new --port command line option to specify the port camelot is to run on

### 1.2.6 (2017-06-16)
#### Fixes
* Fixes an error which may occur while generating reports containing the "Percent nocturnal" column
* Fixes a spurious "insufficient disk space" error which may arise when bulk import is used in a new installation
* Fixes a bug which allows trap station latitude and longitude to be blanked via the advanced menu; these are required fields.

### 1.2.5 (2017-05-13)
#### Enhancements
* Add "delete" as a shortcut key for deleting selected media in the library

#### Fixes
* Fix image data not being removed after media itself has been deleted.
* Fix the location reported for image files in the full export and survey export is incorrect.
* Fix the thumbnail not being loaded when media appears in the library's media sidebar due to the deletion of other media.

### 1.2.4 (2017-04-14)
#### Fixes
* Fix a bug where links to newly uploaded images may be broken.

### 1.2.3 (2017-04-05)
#### Fixes
* Fix a bug where occupancy matrix dates would not be calculated per trap-station.
* Fix a bug which prevented Camelot from starting on Windows when a 32-bit JRE was used.

### 1.2.2 (2017-04-03)
#### Fixes
* Fix a bug which would cause reports to consume much more memory than necessary.

### 1.2.1 (2017-03-18)
#### Enhancements
* Effort summary period start and end now includes the day of the of the month.

#### Fixes
* Fix a bug where some reports will not contain data
* Fix a bug where PRESENCE reports will record periods where no data is available as "0", while such entries should be denoted "-"

### 1.2.0 (2017-02-26)
#### Enhancements
* Improve the responsiveness of the library for large datasets and greatly improve the size of the dataset which can be supported in the Library.
* Improve media selection shortcuts in the library, which includes support for selecting ranges of images with the mouse, while holding the shift key.
* Greatly improve the performance and memory usage of Reports.
* Add ability to delete all selected media in the library.
* Add ability to delete all sightings across selected media in the library.
* Restructure Camelot's `Media` data directory, in order to support large datasets on FAT32.
* Relocate library identification to a floating panel.

#### Fixes
* Fix an error in template generation for bulk import where `:root-path` is unset in `config.clj`.
* Fix a bug which would cause date selection popups to hide every 5 seconds when configuring reports.

### 1.1.1 (2017-02-08)
#### Fixes
* Fixes a bug where changes to the Camera Check, Reference Quality and Attention Needed flags would not be saved.

### 1.1.0 (2017-01-17)
#### Enhancements
* Add Bulk Import functionality: the ability to prepare and import all data for a survey at once.
* New report: Survey Export
* The last selected item in a menu will now be preserved when navigating back to that menu.
* Add the ability to delete media from the Library details panel.
* Add a settings option to change between listing species via scientific names and common names.

#### Fixes
* Prevent browser caching from causing strange incompatibilities after a version upgrade.
* Fix problem where Camelot database may need manual repair should the initial setup or a version upgrade be interrupted.
* Fix a bug which may prevent the currently selected item in menus from highlighting.
* Fix a bug which prevented some browser shortcut keys being used while in the library.
* Fix inconsistencies in date and time display, where the current timezone may spuriously offset the actual time.
* No longer creates an additional downscaled copy of an image for preview.

### 1.0.2 (2016-11-13)
#### Enhancements
* Add display of the current Camelot version at startup.

#### Fixes
* Fix a bug in date select widget which breaks the widget for some timezones.
* Fix some minor date display bugs.
* Fix a bug which may prevent specific configurations of a camera check being submitted.
* Fix a bug where some optional camera trap fields may not be created correctly.

### 1.0.1 (2016-11-05)
#### Fixes
* Fix a critical bug which would prevent Camera Checks from being created.

### 1.0.0 (2016-10-31)
Initial public release.
