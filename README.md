# Camelot

Camelot is open-source camera trapping software for wildlife researchers and conservationists.

The latest version of Camelot is: **[1.1.0](http://camelot.bitpattern.com.au/release/camelot-1.1.0.zip)**

Camelot:

* Makes classifying camera trap photos quick and easy
* Keeps track of camera trap, camera and species data
* Gives you a head start on data analysis
* Plays nicely with other camera trap software, such as [CamtrapR](https://cran.r-project.org/web/packages/camtrapR/index.html) and [PRESENCE](http://www.mbr-pwrc.usgs.gov/software/doc/presence/presence.html)
* Lets multiple people use it at the same time
* Runs on Windows, OSX and Linux
* Is easy to start using

## Prerequisites

### Java runtime

Camelot requires Java 8u91 (on later) to be installed on the system it will run on before it can be used.

Java can be downloaded here: http://www.oracle.com/technetwork/java/javase/downloads/index.html

If using OSX, you will need to install the "JDK".  For Windows and Linux, you can install either the "JRE" or the "JDK".

### Web browser

Camelot supports the latest versions of the following browsers:

* Chrome
* Firefox
* Edge
* Safari
* Internet Explorer 11

## Getting started

### Installation

Download the [latest version of Camelot](http://camelot.bitpattern.com.au/release/camelot-1.1.0.zip).

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
java -jar /path/to/camelot-<version>.jar -server
```

See [Use from multiple computers](#use-from-multiple-computers) for more information.

## Getting help

Camelot has a [detailed user guide](http://camelot-project.readthedocs.io/en/latest/userguide.html), [administration guide](http://camelot-project.readthedocs.io/en/latest/userguide.html) and much more on the [documentation website](http://camelot-project.readthedocs.io/en/latest).

If there's any question you might have that remains unanswered by the documentation, or if you want to stay up-to-date with announcements of new releases, please join the Google Group:

https://groups.google.com/forum/#!forum/camelot-project

## Feature tour and gallery

![](doc/screenshot/your-organisation.png)
Manage and report across multiple surveys from the Your Organisation page.

![](doc/screenshot/library.png)
A beautiful and efficient interface for performing identification quickly.

![](doc/screenshot/camera-check-add.png)
Record camera sessions with ease.

![](doc/screenshot/bulk-import-status.png)
The bulk import is a powerful and fast way of getting existing data into Camelot.

![](doc/screenshot/advanced-menu.png)
Get fine-grained control of your data when you need it.

See the [user guide](http://camelot-project.readthedocs.io/en/latest/userguide.html) to learn more about what Camelot can offer.

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## Acknowledgements
Camelot was developed in consultation with Fauna & Flora International - Vietnam programme.  Input into design was provided by Heidi Hendry, Data Scientist, & Dr Benjamin Rawson, Country Director, of Fauna & Flora International - Vietnam.

With thanks to the Fauna & Flora International camera trappers in Myanmar and Indonesia, especially Wido Albert, Grant Cornette, and Patrick Oswald for detailed feedback about usability and preferred report outputs.  And further thanks to all the Fauna & Flora International Camelot Beta Testers for their support and feedback.
