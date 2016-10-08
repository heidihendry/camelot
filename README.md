# Camelot

Camelot is an open-source, web-based tool to help wildlife conservationists use camera traps in research.

Camelot:

* Makes classifying camera trap photos quick and easy
* Keeps track of camera trap, camera and species data
* Gives you a head start on data analysis
* Plays nicely with other software, such as [CamtrapR](https://cran.r-project.org/web/packages/camtrapR/index.html).
* Lets multiple people use it at the same time
* Runs on Windows, OSX and Linux
* Is easy to start using

## Getting Started

Download the [latest version of Camelot](http://camelot.bitpattern.com.au/release/camelot-0.6.7.zip).

Unzip the archive.  To run Camelot:

*Windows*: Double click `camelot-desktop.bat`

*OSX*: Double click `camelot-desktop.command`

*Linux*: Double click `camelot-desktop.sh`

After 10 seconds, Camelot should appear in a new tab in your web browser.  If Camelot doesn't open automatically, you can access it via your web browser be browsing to:

```
http://localhost:5341/
```

## User guide

### Concepts in Camelot

The first time you run Camelot, you'll be taken to the 'Create Survey' screen.  Now would be good time to explain what a survey is, and some other important concepts in Camelot.

* *Survey*: a survey corresponds to a research project.  All of the data collected will be part of a survey.
* *Camera*: a single, physical camera.  Each camera should be given a name and labelled.
* *Camera Trap Station*: one or two cameras installed at a very specific location.
* *Site*: a geographic area.  Typically multiple camera trap stations will be in each site.
* *Deployment*: a deployment corresponds to a camera trap station which is active in the field.
* *Media*: a photo from a camera trap.

Okay, with terminology out of the way, on to creating a survey!

### Creating a survey

The left hand side is the current survey configuration.  You can give a survey a name and description.  A survey will often start with one or more species are expecting to be found over the course of the study.  Species can be added by searching for the scientific name using the right-hand panel.  Behind the scenes, Camelot will automatically set additional details about the species, including its family and common name.

Once ready, click "Create Survey".

### Your organisation

This is where understanding the concepts in camelot is invaluable.  On this menu, concepts are on the left, and the details about the selected concept are on the right.

You'll notice Sites and Cameras are not *within* a survey.  This allows for some more sophisticated, *longitudinal* reports, where data is not only able to be analysed by survey, but also across multiple surveys.  For the same reason, reports live in this menu too.

Each concept can be selected from the menu on the left, and then a specific entry navigated to using the menu on the right.  You'll find this is a common pattern in Camelot.

#### Surveys

Surveys you'll already be familiar with.  The right hand side shows all surveys, and allows you to add a survey.  You can click on any survey in the list to manage it, but let's first look at the other concepts.

#### Sites

The sites menu will have a very similar feel to the survey menu.  The main difference is that a site can be created just by entering a name and clicking "Add".  This lets you set up multiple sites very quickly, but if you want to come back and provide more information, you can click on the entry for that site in the list to access all the details.

#### Cameras

The camera menu is almost identical in behaviour to the sites menu.  Cameras are added just by entering its name and pressing add, though you're free to give more details too.  After creating a camera you'll notice a label alongside each camera (most likely "Available for use" if you've just added one).

A handy feature is being able to filter the list to find all cameras with a particular status or camera name.  For example, imagine you have dozens of cameras, many of which are in the field, you can search "available" to see the cameras marked "Available For Use".

#### Reports

A camelot report is an export of data to a CSV.  Clicking on a report will take you to a report configuration screen, where you can set constraints for that report (e.g., to report on a specific survey) and then generate the data as a CSV.

Camelot comes with a bunch of reports out of the box, though for advanced users, it also helps you to build and add your own reports.

### Managing surveys

#### Manage camera trap stations

#### Upload captures

#### Species

#### Related files

### Library

#### Viewing photos

#### Photo details

#### Classifying photos

#### Searching

#### Reference window

#### Keyboard shortcuts

So that trap photos can be processed efficiently, the Library has several keyboard shortcuts:

* **Control + m**: Focus the media collection panel
* **Control + d**: Toggle the details panel
* **Control + i**: Open the identify panel
* **Control + Left arrow**: Go to the previous page of media
* **Control + Right arrow**: Go to the next page of media
* **Control + f**: Focus the filter text input
* **Alt + f**: Reapply the current filter

With the Media Collection panel focused (**Control + m**):

* **"wasd"** and **Arrow keys** can be used to select the next media in that direction.
* **Control + a**: Select all media (or select none, if all are selected)
* **f** flag the currently selected media
* **g** mark the currently selected media as processed
* **r** mark the currently selected media as being of reference quality
* **c** mark the currently selected media as a camera-check (i.e., test-fire of the camera)
* hold **shift** + **"wasd"** OR **arrow keys** to select the next media in that direction, and also keep the existing selection.

### Advanced menus

## Administration and advanced configuration

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

## Custom Reports

Custom reports and column definitions for reports can be registered by creating a *reports module*. A reports module can also override existing reports and columns.

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
  [state {:keys [survey-id]}]
  {:columns [:media-id
             :taxonomy-label
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

;; The design of the configuration page for the report.
(def form-smith
  {:resource {}
   :layout [[:survey-id]]
   :schema {:survey-id
            {:label "Survey"
             :description "The survey to report on"
             :schema {:type :select
                      :required true
                      :get-options {:url "/surveys"
                                    :label :survey-name
                                    :value :survey-id}}}}})

(module/register-report
 :custom-report
 {:file-prefix "cool custom report"
  :output report-configuration
  :title "Cool Custom Report"
  :description "A very cool report"
  :form form-smith
  :by :species
  :for :survey})
```

Camelot will treat your field differently when it comes to generating the report, depending on how it the field is named.

* Fields ending in "-id" are converted to Java Longs.
* Fields ending in "-date" are converted to Joda Dates.
* Fields ending in "-float" are converted to Java Floats.
* Fields ending in "-num" are converted to a suitable type. Check the `edn/read-string` documentation for details.

For more module examples, check out Camelot's [built-in reports and columns](https://bitbucket.org/cshclm/camelot/src/master/src/clj/camelot/report/module/builtin/?at=master)

## License

Copyright © 2016 Chris Mann

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
