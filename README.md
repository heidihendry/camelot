# camelot
Camelot is software for Camera Trap data management and analysis.

## Getting Started

One of the goals of Camelot is to be highly configurable.  Before using Camelot, you must generate a configuration file explicitly:

```sh
java -jar camelot-<version>.jar init
```

Camelot will provide the path name to the generated configuration.  You should edit this file to suit your needs before using Camelot.  The following section describes each property available in this file.

### Configuration

#### :required-fields

:required-fields is a list of properties which must be present in the metadata of a file.  Should any one of these properties not be present, a problem will be flagged in the dataset.

This is a list (i.e., `[...]`) of paths into a file's metadata.  The metadata of a file is hierarchical, and so path is a list designating the outermost property through to the innermost in this hierarchy.

```clojure
{...
:required-fields [[:location :gps-longitude]
                  [:datetime]]
...
}
```

Would mean that both the `gps-longitude` within the `location` collection of properties, and the `datetime` must be present in the metadata of every file.

#### :project-start

A timestamp indicating the beginning of the project. Should the timestamp of any file in the dataset fall occur before this, a problem will be flagged.  The start time is _inclusive_.

The string format is `YEAR-MONTH-DAY HOUR:MINS:SECS`

#### :project-end

Like :project-start, but for the project end date.  The end time is _exclusive_.

#### :surveyed-species

A list (i.e., `[...]`) of strings with the names of the species in the survey.  Should any file's metadata include a species not present in this list, a problem will be flagged.

`HUMAN-CAMERACHECK` is considered by Camelot as a special species used to verify the start and end of a phase.  Should a collection not contain a at least 2 files with this species, with unique dates, it will be flagged as a problem in the dataset.

The list is case insensitive.

```clojure
{...
:surveyed-species ["Yellow Spotted Cat",
                   "Smiley Wolf",
                   "HUMAN-CAMERACHECK"]
}
```

#### :language

Language used by Camelot. Currently only `:en` is supported.  Support for `:vn` is planned in the future.

#### :night-start-hour

One of four properties used to (naively) identify camera time configuration issues.  This is the hour of the day at which night is guaranteed to have fallen.  This should be set to the hour after there is no sign of daylight.

#### :night-end-hour

One of four properties used to (naively) identify camera time configuration issues.  This is the hour of the day at which night may have ended.  This should be set to the earliest time at which there's sign of daylight.

#### :infrared-iso-value-threshold

One of four properties used to (naively) identify camera time configuration issues.  The ISO value of the cameras which is used to suggest a photo was taken at night.

File ISO values greater than this threshold are considered `night` photos and thus are expected to lie within :night-start-hour and :night-end-hour

#### :erroneous-infrared-threshold

One of four properties used to (naively) identify camera time configuration issues. This is the maximum allowable proportion of photos which are `night` photos, but do not fall within the block of time denoted by `:night-start-hour` and `:night-end-hour`.

#### :sighting-independence-minutes-threshold

The number of minutes after a species is sighted before further photographs of that species at that location should be considered independent (and thus included in analysis).

*Important:* Currently location is considered as being unique to a folder.  The dependence of two folders at the same location is not currently recognised.

#### :rename

*Important:* This functionality is not currently user-accessible.

Rename files based on their metadata.  This setting consists of 3 properties:

##### :date-format

A string representing the format for which date metadata should be represented.  When setting this, be mindful of characters which may or may not be allowed in filenames (e.g., `:` is problematic).

##### :fields

A list of paths into file metadata (see `:required-fields` for more information about paths) for which data must be extracted to generate the filename.

##### :format

A (format string)[https://en.wikipedia.org/wiki/Printf_format_string] denoting the filename format.

The variables in this are expanded based on the information in the `:fields` property.

There must be exactly as many parameters in this as paths in the `:fields` property.

## Usage

Camelot currently features two modes of operation: a very primitive web interface, and a simple command line data checker.  Of the two, only the command line version is operable.

A Camera Trap project can be analysed with:

```sh
java -jar camelot-<version>.jar bscheck /path/to/allphotos
```

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

## Trying it out

If all is well you now have a browser window saying 'Hello Chestnut',
and a REPL prompt that looks like `cljs.user=>`.

Open `resources/public/css/style.css` and change some styling of the
H1 element. Notice how it's updated instantly in the browser.

Open `src/cljs/camelot/core.cljs`, and change `dom/h1` to
`dom/h2`. As soon as you save the file, your browser is updated.

In the REPL, type

```
(ns camelot.core)
(swap! app-state assoc :text "Interactivity FTW")
```

Notice again how the browser updates.

### Lighttable

Lighttable provides a tighter integration for live coding with an inline
browser-tab. Rather than evaluating cljs on the command line with weasel repl,
evaluate code and preview pages inside Lighttable.

Steps: After running `(run)`, open a browser tab in Lighttable. Open a cljs file
from within a project, go to the end of an s-expression and hit Cmd-ENT.
Lighttable will ask you which client to connect. Click 'Connect a client' and
select 'Browser'. Browse to [http://localhost:3449](http://localhost:3449)

View LT's console to see a Chrome js console.

Hereafter, you can save a file and see changes or evaluate cljs code (without saving a file). Note that running a weasel server is not required to evaluate code in Lighttable.

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

## Deploying to Heroku

This assumes you have a
[Heroku account](https://signup.heroku.com/dc), have installed the
[Heroku toolbelt](https://toolbelt.heroku.com/), and have done a
`heroku login` before.

``` sh
git init
git add -A
git commit
heroku create
git push heroku master:master
heroku open
```

## Running with Foreman

Heroku uses [Foreman](http://ddollar.github.io/foreman/) to run your
app, which uses the `Procfile` in your repository to figure out which
server command to run. Heroku also compiles and runs your code with a
Leiningen "production" profile, instead of "dev". To locally simulate
what Heroku does you can do:

``` sh
lein with-profile -dev,+production uberjar && foreman start
```

Now your app is running at
[http://localhost:5000](http://localhost:5000) in production mode.
## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## Chestnut

Created with [Chestnut](http://plexus.github.io/chestnut/) 0.10.0 (d61a0892).
