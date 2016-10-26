# Camelot

Camelot is an open-source, web-based tool to help wildlife conservationists with camera trapping.

The latest version of Camelot is: **[0.8.3](http://camelot.bitpattern.com.au/release/camelot-0.8.3.zip)**

Camelot:

* Makes classifying camera trap photos quick and easy
* Keeps track of camera trap, camera and species data
* Gives you a head start on data analysis
* Plays nicely with other software, such as [CamtrapR](https://cran.r-project.org/web/packages/camtrapR/index.html).
* Lets multiple people use it at the same time
* Runs on Windows, OSX and Linux
* Is easy to start using

## Prerequisites

Camelot requires Java 8u91 (on later) to be installed on the system it will run on before it can be used.

Java can be downloaded here: http://www.oracle.com/technetwork/java/javase/downloads/index.html

If using OSX, you will need to install the "JDK".  For Windows and Linux, you can install either the "JRE" or the "JDK".

## Getting Started

### Installation

Download the [latest version of Camelot](http://camelot.bitpattern.com.au/release/camelot-0.8.3.zip).

Unzip the archive.  To run Camelot:

**Windows**: Double click `camelot-desktop.bat`

**OSX**: Double click `camelot-desktop.command`

**Linux**: Double click `camelot-desktop.sh`

After 10 seconds, Camelot should appear in a new tab in your web browser.  If Camelot doesn't open automatically, you can access it via your web browser by browsing to:

```
http://localhost:5341/
```

If running Camelot on a server, you can instead use:

```
java -jar /path/to/camelot-<version>.jar
```

## User guide

### Concepts in Camelot

The first time you run Camelot, you'll be taken to the 'Create Survey' screen.  Now would be good time to explain what a survey is, and some other important concepts in Camelot.

* **Survey**: a survey corresponds to a research project.  All of the data collected will be part of a survey.
* **Camera**: a single, physical camera.  Each camera should be given a name and labeled.
* **Camera Trap Station**: one or two cameras installed at a very specific location.
* **Site**: a geographic area.  Typically multiple camera trap stations will be in each site.
* **Media**: a photo from a camera trap.

With terminology out of the way, onwards to creating a survey!

### Creating a survey

The left hand side is the current survey configuration.  You can give a survey a name and description.  A survey will often start with one or more species are expecting to be found over the course of the study.  Species can be added by searching for the scientific name using the right-hand panel.  Behind the scenes, Camelot will automatically set additional details about the species, including its family and common name.

![](doc/screenshot/survey-create.png)

Once ready, click "Create Survey".

### Your organisation

This is where understanding the concepts in camelot is invaluable.  On this menu, concepts are on the left, and the details about the selected concept are on the right.

You'll notice Sites and Cameras are not *within* a survey.  This allows for some more sophisticated, *longitudinal* reports, where data is not only able to be analysed by survey, but also across multiple surveys.  For the same reason, reports live in this menu too.

![](doc/screenshot/your-organisation.png)

Each concept can be selected from the menu on the left, and then a specific entry navigated to using the menu on the right.  You'll find that this is a common pattern in Camelot.

#### Surveys

Surveys you'll already be familiar with.  The right hand side shows all surveys, and allows you to add a survey.  You can click on any survey in the list to manage it, but let's first look at the other concepts.

#### Sites

The sites menu will have a very similar feel to the survey menu.  The main difference is that a site can be created just by entering a name and clicking "Add".  This lets you set up multiple sites very quickly, but if you want to come back and provide more information, you can click on the entry for that site in the list to access all the details.

Adding details to a site is just a matter of filling in the fields and clicking "Update".

![](doc/screenshot/site-edit.png)

#### Cameras

The camera menu functions almost identically sites menu just discussed.  Cameras are added by entering its name and pressing add right in the right-hand menu, though you're free to edit the camera afterwards to give more details too.  After creating a camera you'll notice a label alongside each camera (most likely "Available for use" if you've just added one).

A handy feature is being able to filter the list to find all cameras with a particular status or camera name.  For example, imagine you have dozens of cameras, many of which are in the field, you can search "available" to see the cameras marked "Available For Use".  Camelot ensures the camera status is updated as it is used in, and removed from, camera trap stations.

#### Reports

A *report* is an export of data to a CSV.  Clicking on a report will take you to a report configuration screen, where you can set constraints for that report (e.g., to report on a specific survey) and then generate the data as a CSV.

Camelot comes with a bunch of reports out of the box.  For advanced users, it also lets you build and add your own reports.  Reports will be covered in much more detail later.

### Managing surveys

Survey management is the heart of Camelot, and also where it differs the most from other camera trap software.  Our research shows that by understanding the next few sections, you'll be well on your way to being a Camelot expert.  So listen up!

The authors recommend physically setting up the cameras, and noting the installation details, before recording those details in Camelot.  If you can record the details into Camelot while in the field, even better!

#### Manage camera trap stations

This menu shows all past and present camera trap stations, and also allows you to add another camera trap station to the field.  A quick refresher: a camera trap station is one or two cameras at a specific location.

There are two pages in setting up a new camera trap.  The first page has the *essential* information.  All of the fields on this screen must be completed, with the exception of the "secondary camera" in the case there was only 1 camera installed.  While on the subject of cameras, don't be concerned about the distinction between "primary" and "secondary" cameras: Camelot is a fair master and treats both cameras equally.

![](doc/screenshot/camera-trap-add.png)

Once you're happy with all of the essential details, click "Next".  You can now set up any of the optional data for the camera trap.  This should be pretty self-explanatory.  This data, if provided, would usually be for reporting and analysis purposes later.  Once you're happy with everything, click "Create".

If you've used other camera trap software, you may now be starting to notice some differences: the trap station, and the start date, and the cameras -- they were all on the one screen!  What if you need to change cameras later?  Do not fear, Camelot supports all of this.  Read on!

After creating a camera, you will see it under the "Manage camera trap stations" menu.  All camera traps will be shown on this menu, with camera traps which are no longer active in the field being marked as "Finalised".  Each of these cameras can be clicked on to view its details, or if not finalised, to record details about a *camera check*.

![](doc/screenshot/camera-trap-manage.png)

So that's a piece of terminology which has not been introduced until now: a **camera check** is the process of going to a camera trap station, making sure the cameras are okay, collecting the media they've taken and doing any maintenance required (e.g., replacing batteries).

After checking a camera trap station and collecting the media, this information must be entered into Camelot.  Recording a camera check requires 3 pieces of data:

* What date was the camera check performed?
* What happened with the "primary" camera?
* What happened with the "secondary" camera?

![](doc/screenshot/camera-check-add.png)

Camelot facilitates the following scenarios, for each of the cameras:

* Whether or not media was successfully recovered from the camera.
* If the camera was lost/stolen/retired or was taken to be used elsewhere ("available for use").  This will *unassign* that camera from the camera trap station.
* If the camera was previously installed and is still installed, it remains "Active in the field".
* If that camera was replaced with another, you can specify the replacement camera or indicate "No replacement camera"

Also, if there wasn't previously a second camera installed, a new secondary camera can be selected.

When all of the details are correct, click "Submit".

When submitting, one of two things will happen:

1. If there were still cameras assigned to that trap station (i.e., either a camera was still "Active in the field", or a new camera was assigned), Camelot will mark the camera check date as the last date of a the session, and start a new session on this date.  Or,

2. If a check was submitted where there are now no cameras at a trap station, that trap station is no longer active and will no longer be available for management.  If this happens there's nothing stopping you from adding a new camera trap station at that location later on, but right now, Camelot knows photos aren't being taken, and will take care of finishing it up for you.

Phew, okay that was a lot to take in!  Reward yourself with a cup of tea and biscuit.  The main take away is that Camelot will do a stack of behind-the-scenes book-keeping work for you.  Managing camera trap stations is all about telling Camelot what happened, and it will take care of the rest for you.

#### Upload media

That last section went on for *ages* about managing camera trap stations, and doing checks and collecting media, but didn't say how to actually upload the photo!  What gives?  Well, good news, now we're all set up to start uploading photos.

Click on "Upload media" on the main menu, and Camelot will show a list of cameras we've collected media for.  Camelot knows this because we've submitted a camera check, for that camera, and we said we had "recovered the media".  (If this is all double-dutch to you, see the section on "Managing Camera Trap Stations" above.)

![](doc/screenshot/media-upload.png)

To upload the media, it's as simple as opening your file browser, selecting all the files taken by that camera over the time period shown, and dragging them on to it!  Camelot will show a progress bar, telling you how the upload is going.  If there are any problems, a "Show Details" link will appear below the progress bar, which will give you the low-down.

You can upload multiple sets of files to a camera, and even be uploading to many cameras at once.  You should not leave the "Upload media" menu while photos are uploading, as doing so may interrupt the upload.

Once all of the uploads are complete -- you will know this, as all of the progress bars will be full -- you can head on to the *Library* to start identification.

#### Species

This provides a list of species either expected or encountered during a survey.  You can click on any species in the list in order to edit the details about the species, such as its common name, family and mass category.  Note that changes made to the species details here will apply to all surveys.

To change the species available in a survey, click "Manage Species" towards the bottom of the right hand panel.  From this menu, you'll be able to remove species using the menu to the left and add species using the options to the right.

Removal is simple: if you want to remove a species, click "Remove".  Removing a species will only change its availability in the identification dropdown for a survey.  If photos in the survey have already been identified with this species, they will still be, and as such these species will also appear on reports for the survey.

![](doc/screenshot/species-manage.png)

To add a species, there's a bit more involved.  Camelot gives 3 different ways to add a species:

* Select the species from the dropdown, if it's already known to Camelot.
* Search by scientific name, when a species is not in the dropdown and you would like the details about the species, like Common name and Family, automatically set up.
* The final option is useful when a species hasn't been added to Camelot before, and isn't available in the search.  Select "add a new species..." from the dropdown, type the scientific name of the species, and click "Add".

Click "Done" to save your changes.

#### Related files

It's nice to be able to keep related data all in the same place, and this is the goal of the related files.  You can upload any file you like to Camelot using the file picker towards the bottom of the window, and it will be added to the list of files for this survey and available to view anytime you like.

### Library

Okay, so you've had camera traps in the field for a while and collected a bunch of photos.  Now's where the *library* comes in.  All of your photos can be viewed, searched, flagged and identified through the library.

#### Viewing photos

The "viewing" portion of the library consists of 3 main parts:

![](doc/screenshot/library.png)

1. A photo can be *selected* by clicking on the photo on the "media collection" view on the left.  When a photo has a green border, it is a selected photo.
2. When selecting a photo, a preview for it will be displayed in the preview panel in the middle of the screen.
3. Details for the currently viewed photo are available by clicking the "Details" tab on the right of screen to expand that panel.

Selecting is an important concept in the library, as most actions will apply to the current selection.  Multiple photos can be selected by holding the "control" key and clicking a photo.  Allowing multiple photos to be selected is one technique Camelot uses to make processing of photos quicker and easier.

Camelot's media collection shows images in page of 50.  Clicking the left and right arrows immediately above will change the page of photos displayed.  "Selecting all" will select all photos on the page.

A photo can be viewed without being selected (useful if you have multiple photos selected already) by clicking the "eye" icon in the top left hand corner of each photo in the media collection panel.

Finally, you might notice some photos have either a blue or red border around them.  A blue border indicates the image is "processed", while a red border is "attention needed".  If an image is flagged as processed and attention needed, the border will be red; attention needed takes precedence.

#### Flagging photos

A flag is one of the four icons towards the top right of the library:

![](doc/screenshot/library-flags.png)

From left to right these are:

* **Needs Attention**: mark the photo is needing further review.
* **Test fire**: the photo was triggered by someone attending to or testing the camera.
* **Processed**: identification of the photo is complete.  If a photo doesn't show any species, it can and should be marked as processed without any species being identified in it.
* **Reference Quality**: the photo has a species in it, and the photo of that species is a great example to refer to when doing future identification. (See: "Reference Window")

These can be set and unset by clicking on the respective icon.  It will apply to all photos currently selected.

#### Identifying photos

Identification in Camelot is the process of indicating which species are present in a photo.  When you're ready to identify the selected photos, the mode for submitting details can be accessed via the "Identify Selected" button in the top right hand corner of the library.  To switch back to searching, just press the Search button in the same location.

To submit an identification, set the species from the drop down, adjust the quantity if necessary, specify other identifiable details from the appropriate drop down and click "Submit".  The identification bar will disappear and the photo will automatically be marked as "Processed".

![](doc/screenshot/library-identify.png)

Sometimes you'll come across a species you didn't expect to see in a survey, and haven't encountered before.  You can add this right away by using the "Add a new species..." option in the species dropdown, and then typing the species scientific name in the input field which appears.  This species will also be added in case it's needed in future.  Quick and easy.

If you need to know which species have already identified in a photo, these can be viewed (and removed) via the details panel.  If there aren't any species identified in a photo, there will not be any listed in the details panel for that photo.

The identification details entered will be applied to all photos currently selected.

#### Searching

![](doc/screenshot/library-search-bar.png)

The search bar will change the photos shown in the media collection to only those which match the search.  The search bar has a few common search constraints readily-available: the survey, the trap station and a checkbox to show only unprocessed.  And of course a text input field where you can just type and press the search button (or "enter").

The text input field deserves a little bit more explanation.  It can be used for simple searches, like typing the name of a camera to find all photos taken by it, or the name of a species (or genus).  But it can also be used for much more specific searches.

If you wanted to find all photos at a certain sublocation, you could start typing:

```
site-s
```

![](doc/screenshot/library-search.png)

At this point you should see completions below the input field for "site-sublocation" and "site-state-province".  Click "site-sublocation" to complete it for you, and, if you had set up some sublocations for sites previously, you'll notice another drop down: all of the sublocations in Camelot.

Sometimes you want to search based on more than one thing.  For example, all photos at a site AND featuring a certain species.  Can do:

```
site-name:uluru species:"Osphranter rufus"
```

Camelot can also do searches with disjunctions:

```
site-name:uluru | species:"Osphranter rufus"
```

Note the pipe ('|') in the above example: this means "OR" in a search.

We think Camelot's search is pretty handy, and hope you think so too.

#### Reference window

The reference window is used to help with difficult identifications and make identification more accurate by using photos you have already classified.  By clicking the reference window, Camelot will open a new, specialised version of the library in another window which will display only photos marked as "Reference Quality" (see "Flagging Photos").

Once opened, the media available through in the Reference Window will change depending on the species selected in the identification dropdown in the main Camelot window.  This can also be useful if there are a several possible species: by selecting the different species in the species identification drop down in the main window, you can quickly preview other photos identifying this species.

A possible workflow is that clear photos are identified first and marked "Reference Quality" where appropriate.  Photos which are not so easy to identify can be marked as "Attention Needed".  After making a pass through the easy identifications, you can then come back and use the Reference Window, and quality photos you have already collected, to help with making identifications of species in those more tricky photos.

This window is designed to be put on a second monitor, but if that is not available, can also be accessed quickly via "Alt-Tab".

#### Keyboard shortcuts

So that trap photos can be processed efficiently, the Library has a number of keyboard shortcuts:

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
* hold **shift** + **"wasd"** OR **arrow keys** to include more media, keeping the existing selection.

### Settings

In the top right hand corner of the main navigation is the toggle to show the settings menu.  The settings menu in Camelot currently provides two options:

![](doc/screenshot/settings.png)

* **Sighting Independence Threshold**: Camelot performs sighting independence checks on some reports.  This setting controls the duration within which photos of the same species (and, if known, the same sex and life-stage) are considered dependent in those reports.
* **Send anonymous usage data**: Enabled this option will provide data about how you use Camelot back to the Camelot Project. This data is anonymous and is used only for the purpose of improving Camelot. It is disabled by default, though if you have the consent of others using your Camelot instance, please consider enabling this.

### Reports

#### Calculated columns

Some columns in Camelot are calculated from existing data, and some of those calculations are worthy of some explanation so that it's clear what that column represents.  Here are the most interesting ones:

##### Independent observations
A sighting is considered independent if two photos with the same sighting are taken at least some time threshold apart.  If they are not, it is considered dependent.  Whether they are considered dependent or independent effects the value of the data in this column.

Photos may be considered dependent if they are within the same Trap Station Session.  That is to say, photos taken by two cameras of a Trap Station at the same time, if for the same sighting, will be **dependent**.

Below are some examples showing the various rules of the calculation (assume T=30 as the threshold).  All rules assume sightings are within the same Trap Station Session; if that were not the case, they would always be **independent**.

| Sighting | Quantity | Lifestage | Sex | T    |
| :------- | :------- | :-------- | :-- | :--- |
| Spp. 1   | 1        | Adult     | Male| 0    |
| Spp. 2   | 1        | Adult     | Male| 5    |

These are **independent** as it's a different species.  The number of Independent Observations is **2**.

| Sighting | Quantity | Lifestage | Sex | T    |
| :------- | :------- | :-------- | :-- | :--- |
| Spp. 1   | 1        | Adult     | Male| 0    |
| Spp. 1   | 1        | Adult     | Male| 40   |

These are **independent** as while it's the same species, it is separated by T=40.  The number of Independent Observations is **2**.

| Sighting | Quantity | Lifestage | Sex | T    |
| :------- | :------- | :-------- | :-- | :--- |
| Spp. 1   | 1        | Adult     | Male| 0    |
| Spp. 1   | 2        | Adult     | Male| 5    |

These are **dependent** as it's the same species, and up to 2 were sighted within the dependence window.  The number of Independent Observations is **2**.

| Sighting | Quantity | Lifestage | Sex | T    |
| :------- | :------- | :-------- | :-- | :--- |
| Spp. 1   | 1        | Adult     | Male| 0    |
| Spp. 1   | 1        | Juvenile  | Male| 5    |

These are **independent** as while it's the same species, one is a juvenile and the other an adult.  The number of Independent Observations is **2**.

| Sighting | Quantity | Lifestage     | Sex         | T    |
| :------- | :------- | :------------ | :---------- | :--- |
| Spp. 1   | 1        | Adult         | Male        | 0    |
| Spp. 1   | 1        | Unidentified  | Unidentified| 5    |

These are **dependent** as while the lifestage and sex are not the same, unidentified values are *inferred*.  The number of Independent Observations is **1**.

| Sighting | Quantity | Lifestage     | Sex         | T    |
| :------- | :------- | :------------ | :---------- | :--- |
| Spp. 1   | 1        | Unidentified  | Unidentified| 0    |
| Spp. 1   | 1        | Adult         | Male        | 5    |
| Spp. 1   | 1        | Unidentified  | Female      | 10    |

Sighting 2 of Spp. 1 is **dependent** on sighting 1, due to inference on Lifestage and Sex.  Sighting 3 is **independent** of both sighting 1 and sighting 2 due to that inference.  The number of independent observations is **2**.

The value of the threshold is defined in Camelot's settings menu ("Independent Sighting Threshold (mins)").

#### Nocturnal (%)

This is simply the number of photos taken at night, divided by the number of photos.  The interesting part is what is considered to be "at night".

Night is determined as a time after sunset and before sunrise, given a particular set of GPS coordinates and on a particular day using the sunrise and sunset times as calculated by [an algorithm published by the Nautical Almanac Office](http://williams.best.vwh.net/sunrise_sunset_algorithm.htm).  This algorithm does not attempt to account for atmospheric or geographical features, though will typically be accurate to within several minutes of the actual sunrise and sunset times.

#### Abundance Index

The Abundance Index is calculated using two pieces of data: the number of number of independent observations, and the number of nights of elapsed operation for a camera trap session, or for the combined elapsed time of all camera trap sessions (depending on the report).

The calculation of this value is then:

`100 * Independent Observations / Nights`

### Advanced menu

Occasionally in Camelot you may find "Advanced" buttons, like this one in the Surveys menu:

![](doc/screenshot/advanced-button.png)

You might want an Advanced menu should you find an unusual scenario arise, and you find yourself needing to bypass the usual workflows.  For example, when attending to a trap station, it is found that the cameras between it and a nearby trap station had been mixed up.  As there are many unusual scenarios just like this, Camelot caters for them with a series screens which give fine-grained control over the data.

![](doc/screenshot/advanced-menu.png)

An advanced menu screen is specific to one type of data.  In the above, the screen is specific to Trap Stations.  All advanced menus follow the same pattern:

1. On the left is the Sidebar.  The sidebar shows you a list of entries.  This list contains everything Camelot knows about that type of data (for example, everything Camelot knows about Trap Stations).  You can click on an item in the sidebar to view it, or use the "**+**" button at the top to create a new entry.

2. While viewing an entry, you can see a drop down menu which provides a series of *Actions*.  This dropdown is very important when working in the advanced mode, and nearly all operations in the advanced mode use this menu, including navigation! In the above example, this dropdown menu it allows for Deleting the selected entry, Editing it, and Viewing its associated *Sessions*.

#### Navigating the advanced menu

Let's take a step back, and have a look at a map of Camelot's advanced menus:

```
          Surveys
             |
             v
         Survey Sites  <---------- Sites
             |
             v
        Trap Stations
             |
             v
        Trap Station
          Sessions
             |
             v
        Trap Station   <--------- Cameras
       Session Cameras
             |
             v
           Media
            / \
           /   \
   Photo <'     `> Sightings <--- Species
 Metadata
```

Phew!  Hopefully you'll agree that Camelot's normal interface sure does simplify this.

The most important things to take away are these:

* Survey, Site, Camera and Species are the roots of the data structure.  To get to these, you must go directly to their Advanced menu.  Advanced menus for everything else can be, and often are, accessed via other Advanced menu.  (The exception is that there's is an Advanced menu button in the Camera Trap Station details screen, which is a handy shortcut!)
* To go from the Survey level, to the Media or Sightings levels requires going through each level in between.

The pattern to go from one level to the next is:

* select from the Sidebar the entry you want to descend in to, then,
* select the *View ...* option from the Actions drop-down menu.

#### Example usage

Coming back to the example given earlier on when and where to use the Advanced menus: we've just realised that the cameras assigned two trap station (sessions) have been mixed up!

Here's a step-by-step process on how you might go about fixing that:

1. Organisation -> Cameras -> Create a new camera called "Temporary".
2. Access the Advanced menu for Problem Trap Station #1: Organisations -> Surveys -> MySurvey -> Manage Camera Trap Stations -> MyTrap1 -> Details -> Advanced
3. Select the session from the Sidebar where the cameras are known to be wrong
4. Actions -> View Cameras
5. Select the camera wrongly assigned, then Actions -> Edit
6. Set the Camera to "Temporary", as we created earlier
7. Access the Advanced menu for Problem Trap Station #2: Manage Camera Trap Stations -> MyTrap2 -> Details -> Advanced
8. Now we repeat the process: select the session, Actions -> View Cameras, select the wrongly assigned camera, Actions -> Edit
9. Assign the camera we unassigned from Problem Trap Station #1.
10. And we repeat the process again: select the session, Actions -> View Cameras, select the wrongly assigned camera, Actions -> Edit
11. Assign the camera unassigned from Problem Trap Station #2 to complete the swap.
12. Finally, Organisation -> Cameras -> Remove "Temporary" to clean up the temporary camera.

#### Parting advice

Camelot tries hard to keep workflows simple and logical, though inevitably there'll times where you really need to get your hands on the data to make things right.  Camelot provides for both scenarios, but do not be complacent when using the advanced menus -- Camelot will (deliberately) not help you.

Take your time to make sure that side effects of any changes are correctly accounted for.  Session date changed?  Make sure any sessions immediately before or after are updated too.  Camera changed?  Make sure you update the status of the cameras.  If you're ever unsure about the right way to handle a situation, feel free to get in touch via the Google Group.

## The Community

To keep up to date with any new releases, or if you have any questions that haven't been answered here, please join the Google Group:

https://groups.google.com/forum/#!forum/camelot-project

## Administration and advanced configuration

*This section is not for the faint-of-heart, and intended for people with strong IT knowledge.*

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
* **Config**: $HOME/.config/camelot

### Data Directory

The data directory will contain three subdirectories: `Database`, `Media` and `FileStore`.  Database is an Apache Derby database.  Imported media is not stored in the database, but in the `Media` folder.  Finally, the `FileStore` contains files for the Survey's "Related files" feature.

A custom data directory can be set using the `CAMELOT_DATADIR` environment variable.  The Database and Media directories will be created (if necessary) and stored within that nominated directory.  If `CAMELOT_DATADIR` is not set, Camelot will fall-back to using the standard locations (as above).

Each of the `Database`, `Media` and `FileStore` directories should be backed up routinely.

### Config Directory

#### config.clj

`config.clj` is the global camelot configuration file.  All configuration values available in this can also be set through the settings panel in the UI.

### Custom Reports

Custom reports and column definitions for reports can be registered by creating a *reports module*. A reports module can also override existing reports and columns.

Reports modules are Clojure files (`.clj` extension) and are stored under the `modules` subdirectory of Camelot's config directory (described above).

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

For more module examples, check out Camelot's [built-in reports and columns](https://gitlab.com/camelot-project/camelot/tree/master/src/clj/camelot/report/module/builtin/?at=master)

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
