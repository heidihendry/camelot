# camelot
Camelot is software for Camera Trap data management and analysis.

Currently Camelot reports validation problems for survey data read from image metadata.

## Getting Started

Run camelot with:

```sh
java -jar camelot-<version>.jar
```

This starts a web server which is available on TCP 8080 (by default).  If you wish to use another port, set the CAMELOT_PORT environment variable to the port number you desire.

You should connect to this port with your web browser.  Upon connecting, you'll be notified that Camelot is not configured.  Open the settings menu from the top right-hand corner, set the settings you wish to use, and press `Save' from the button left corner of the screen.

## Limitations
This is a 'pre-alpha' release, and thus comes with numerous limitations.

* Only the latest versions of Firefox and Chrome are supported
* Does not support video files
* English is the only supported language

## Validations

Checks and reporting on whether:

* the timestamp on a photo is significantly earlier or later than the norm
* the timestamp on some photos lies outside of the project start and stop times
* the camera exposure settings coincide with the time configuration
* the photos do have a camera check at the beginning and end
* the headline differs across photos in a folder
* one or more required fields are missing from one or more photos
* a sighting is incomplete (i.e., species without quantity or quantity without species)
* a species was identified which is not known to the survey
* a timeshift applied to photos is applied consistently throughout a camera trap operation period's dataset

## Keyboard shortcuts

So that trap photos can be processed efficiently, the Library screen has keyboard shortcuts:

* **Control + m**: Focus the media collection panel
* **Control + d**: Toggle the details panel
* **Control + i**: Open the identify panel
* **Control + p**: Go to the previous page of media
* **Control + n**: Go to the next page of media
* **Control + a**: Select all media (or select none, if some are selected)
* **Control + g**: Toggle the flag state of the selected media
* **Control + h**: Toggle the processed state of the selected media
* **Control + f**: Focus the filter text input
* **Alt + f**: Reapply the current filter

With the Media Collection panel focused (**Control + m**):

* **"wasd"** and **Arrow keys** can be used to select the next media in that direction.
* hold **shift** + **"wasd"** OR **arrow keys** to select the next media in that direction, but also keep the existing selection.

## Configuration

### Required Fields

A list of properties which must be present in the metadata of a file.  Should any one of these properties not be present, a problem will be flagged in the dataset.

### Project Start

A timestamp indicating the beginning of the project. Should the timestamp of any file in the dataset fall occur before this, a problem will be flagged.  The start time is _inclusive_.

### Project End

Like 'project-start', but for the project end date.  The end time is _exclusive_.

### Surveyed Species

A list of strings with the names of the species in the survey.  Should any file's metadata include a species not present in this list, a problem will be flagged.

`HUMAN-CAMERACHECK` is considered by Camelot as a special species used to verify the start and end of a phase.  Should a collection not contain a at least 2 files with this species, with unique dates, it will be flagged as a problem in the dataset.

### Language

Language used by Camelot. Currently only `English` is supported.  Support for `Vietnamese` is planned in the future.

### Night Start Hour

One of four properties used to (naively) identify camera time configuration issues.  This is the hour of the day at which night is guaranteed to have fallen.  This should be set to the hour after there is no sign of daylight.

### Night End Hour

One of four properties used to (naively) identify camera time configuration issues.  This is the hour of the day at which night may have ended.  This should be set to the earliest time at which there's sign of daylight.

### Infrared ISO Value Threshold

One of four properties used to (naively) identify camera time configuration issues.  The ISO value of the cameras which is used to suggest a photo was taken at night.

File ISO values greater than this threshold are considered `night` photos and thus are expected to lie within 'Night Start Hour' and 'Night End Hour'

### Erroneous Infrared Threshold

One of four properties used to (naively) identify camera time configuration issues. This is the maximum allowable proportion of photos which are `night` photos, but do not fall within the block of time denoted by `Night Start Hour` and `Night End Hour`.

### Sighting Independence Minutes Threshold

The number of minutes after a species is sighted before further photographs of that species at that location should be considered independent (and thus included in analysis).

**Important:** Currently location is considered as being unique to a folder.  The dependence of two folders at the same location is not currently recognised.

## Administration and Advanced Configuration

Camelot has two directories: one for configuration, and one for data storage.  The location of these directories depends on the OS.

### Locations
#### Windows

* **Data**: %LOCALAPPDATA%\camelot
* **Config**: %APPDATA%\camelot

#### OSX

* **Data**: $HOME/Library/Application Support/camelot
* **Config**: $HOME/Library/Preferences/camelot

#### Linux

* **Data**: $HOME/.local/share/camelot
* **Data**: $HOME/.config/camelot

### Data Directory

The data directory will contain two subdirectories: `Database` and `Media`.  Database is an Apache Derby database.  Imported media is not stored in the database, but in the `Media' folder.

A custom data directory can be set using the `CAMELOT_DATADIR` environment variable.  The Database and Media directories will be created (if necessary) and stored within that nominated directory.  If `CAMELOT_DATADIR` is not set, Camelot will fall-back to using the standard locations (as above).

Both of the `Database` and `Media` directories should be backed up routinely.

### Config Directory

#### config.clj

`config.clj` is the global camelot configuration file.  All configuration values available in this can also be set through the settings panel in the UI.

#### Custom Reports

Custom reports and column definitions for reports can be registered by creating a *reports module*.     A reports module can also override existing reports and columns.

Reports modules are Clojure files (`.clj` extension) and are stored under the subdirectory `/modules/reports` of Camelot's config directory (described above). If the `/modules/reports` subdirectories don't exist, you will need to create them.

All modules in this directory will be loaded before each report is ran.

Here's an example module to create and register a custom column, and a custom report using that column.

```clojure
(ns custom.camelot.module.custom_column
  (:require [camelot.report.module.core :as module]))

(defn custom-column
  [state data]
  (map #(assoc % :custom-column
               (if (:survey-id %)
                 "YES"
                 "NO"))
       data))

(module/register-column
 :custom-column
 {:calculate custom-column
  :heading "Custom Column"})

(defn report-configuration
  [state survey-id]
  {:columns [:species-scientific-name
             :trap-station-longitude
             :trap-station-latitude
             :custom-column]
   :aggregate-on [:independent-observations
                  :nights-elapsed]
   :filters [#(:trap-station-longitude %)
             #(:trap-station-latitude %)
             #(:species-scientific-name %)
             #(= (:survey-id %) survey-id)]
   :order-by [:species-scientific-name
              :trap-station-longitude
              :trap-station-latitude]})

(module/register-report
 :custom-report
 {:file-prefix "my custom report"
  :configuration report-configuration
  :by :species
  :for :survey})
```

Currently reports are available by directly accessing the URL:

`/report/<my-report-name>/<for-id>`  (e.g., for the above example it would be `/report/custom-report/3`, where `3` is the survey ID.)

In the future they will be made available through the UI.

For more module examples, check out Camelot's [built-in reports and columns](https://bitbucket.org/cshclm/camelot/src/master/src/clj/camelot/report/module/builtin/?at=master)

## Experimental Features

Features which are under heavy development are hidden by default.  These can be enabled by setting the `CAMELOT_DEV_MODE` environment variable to `true`.

## Development

Open a terminal and type `lein repl` to start a Clojure REPL
(interactive prompt).

In the REPL, type

```clojure
(run)
(browser-repl)
```

The call to `(run)` starts the Figwheel server at port 3449, which takes care of
live reloading ClojureScript code and CSS. Figwheel's server will also act as
your app server, so requests are correctly forwarded to the http-handler you
define.

Running `(browser-repl)` starts the Weasel REPL server, and drops you into a
ClojureScript REPL. Evaluating expressions here will only work once you've
loaded the page, so the browser can connect to Weasel.

When you see the line `Successfully compiled "resources/public/app.js" in 21.36
seconds.`, you're ready to go. Browse to `http://localhost:3449` and enjoy.

**Attention: It is not needed to run `lein figwheel` separately. Instead we
launch Figwheel directly from the REPL**

### Emacs/Cider

Start a repl in the context of your project with `M-x cider-jack-in`.

Switch to repl-buffer with `C-c C-z` and start web and figwheel servers with
`(run)`, and weasel server with `(browser-repl`). Load
[http://localhost:3449](http://localhost:3449) on an external browser, which
connects to weasel, and start evaluating cljs inside Cider.

To run the Clojurescript tests, do

```
lein doo phantom
```

## License

Copyright © 2016 Chris Mann

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
