## ChangeLog
### 1.2.0 (upcoming)
#### Enhancements
* Improve the responsiveness of the library for large datasets and greatly improve the size of the dataset which can be supported in the Library.
* Improve media selection shortcuts in the library, which includes support for selecting ranges of images with the mouse, while holding the shift key.
* Greatly improve the performance and memory usage of Reports.
* Add ability to delete all selected media in the library.
* Add ability to delete all sightings across selected media in the library.
* Restructure Camelot's `Media` data directory, in order to support large datasets on FAT32.

#### Fixes
* Fix an error in template generation for bulk import where `:root-path` is unset in `config.clj`.

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
