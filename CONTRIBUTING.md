## Contribution Guide

Welcome to the Camelot project!  Our goal is to provide an intuitive yet powerful platform for wildlife conservation research using camera trapping.  There's a whole variety of skills needed to achieve this and your skills would be very much appreciated.

## How can I contribute?

Here's some specific ways you can help:

### Raising issues for bugs and problems

[Raise an issue](https://gitlab.com/camelot-project/camelot/issues/new) should you encounter a problem or limitation within Camelot.  When reporting a problem, please give the issue a clear and meaningful title and try to provide as much information about the problem itself as possible.

The following is a great template to use.  You can copy and paste this in to the issue description to give yourself a head start.

```
**Description**

*Please describe clearly what the problem is.  If there are errors provided by Camelot, also include the error in its entirety here.*

**Steps to reproduce**

<i>Give a step by step, what you did to encounter the problem.  For example:

1. Start camelot using the `camelot-desktop.command` file.
2. Click "Reports"
3. ...

This lets whoever is looking in to the problem not only work out what is happening, but make sure that it's resolved properly.</i>

**The expected behaviour was**

*Tell us what you thought Camelot would do, or should do, in this scenario.*

**But the actual behaviour was**

*Tell us what Camelot actually did*

**Camelot version**

*e.g., version 1.0.0*

**Additional information**

<i>Any additional information you think might be helpful or relevant.

For example:

* Operating System & version
* Web browser & version
* When you first noticed the problem
* ...
</i>
```

### Requesting features

[Raise an issue](https://gitlab.com/camelot-project/camelot/issues/new) to request a new feature in Camelot.  For a feature to be added to Camelot, it will need to solve a specific problem which would apply to a number of Camelot's users.

When raising a feature request, give it a clear and meaningful title.  For the description, consider using the following template:

```
As a [a user role]
I would like [the goal of the feature]
So that [the benefit of the feature to that user]
```

When raising a feature request, remember the best feature requests clearly capture the problem and the value of solving that problem.

### Documentation improvements

Regardless of how good a piece of software is, it offers no value if it can't be understood.  We want to provide excellent documentation, so if you see a mistake, omission or areas it can be improved, please do let us know.

If you're comfortable with git, we gratefully accept [Merge Requests](https://docs.gitlab.com/ce/gitlab-basics/add-merge-request.html) for documentation improvements.  Otherwise please [raise an issue](https://gitlab.com/camelot-project/camelot/issues/new) describing the documentation change you would like to be included.

### Translations

Currently Camelot has internal support for translations, but has not been translated.  If you know English and another language, translations would be greatly appreciated.

The Camelot project has two different types of resources available for translation:

1. Camelot itself

Building a translation simply involves taking the strings in the [English translation file](https://gitlab.com/camelot-project/camelot/blob/master/src/cljc/camelot/translation/en.cljc) and replacing them with translated strings.  Occasionally strings will contain `%s` or similar; this is a placeholder which gets replaced with other content by Camelot.

2. Project documentation.

Translations for project files, such as the User Guide or this Contribution Guide, are help to make Camelot more accessible to native speakers of other languages.

To contribute new translations or improvements to existing translations back to the project, please either raise a [merge request](https://gitlab.com/camelot-project/camelot/merge_requests) or [raise an issue](https://gitlab.com/camelot-project/camelot/issues/new).

### Code contributions

And of course we accept code contributions. :-)

If you're looking for something to work on, check to see if there are any [outstanding issues](https://gitlab.com/camelot-project/camelot/issues).  If you have "scratched your own itch", please do open a [merge request](https://gitlab.com/camelot-project/camelot/merge_requests), as it would be great if we could include your changes in the project.

If you'd like to help, but are not sure where to start, check out Camelot's built-in reports.  These are relatively easy to get started with, and we would love to add better compatibility with more data analysis products used in research.

Finally, if you're new to Git or GitLab, we recommend you check out the GitLab Basics guide to get started: https://docs.gitlab.com/ce/gitlab-basics/README.html.

## Development

Camelot is implemented in Clojure and ClojureScript.  This section will give some pointers on how to get started with developing Camelot.

### Getting Started with Clojure

There are plenty of resources for working with Clojure; this contribution guide will not cover that.  But here are some links you may find useful:

* http://clojure.org/guides/getting_started
* http://www.braveclojure.com/getting-started/

### Building Camelot

There's only a couple of steps to getting Camelot built from sources\*.

1. Camelot uses Leiningen for managing the project build.  First, follow [the installation instructions](http://leiningen.org/#install) for Leiningen.

2. Once installed, building Camelot is one command\*\*:

```
./script/clean-build.sh
```

This will produce artifacts in /target.  To run the new build, use:

```
java -jar target/camelot.jar
```

If you encounter problems, raise an issue or reach out via the Google Group.

\* Camelot is designed to be developed within a \*nix environment (e.g., Linux or OSX).  It may be possible to follow this process within Windows using tools such as msys2.

\*\* Building and running artifacts probably shouldn't be your normal dev-loop.  Most Clojure developers prefer to work interactively at a REPL.

### Running tests

Camelot currently has 3 test suites, one for .cljc, one for clj, and one for cljs.

The commands to invoke them are, respectively:

* `lein with-profiles +test test`
* `lein with-profiles +test midje`
* `lein doo phantom test once`

(If someone is keen to raise a merge request to tidy this up, that would be awesome!)

### Working at the REPL

Open a terminal and type `lein repl` to start a Clojure REPL
(interactive prompt).

To run Camelot  from the REPL:

```clojure
(run)
```

The call to `(run)` starts the Figwheel server at port 3449, which takes care
of live reloading ClojureScript code and CSS. Figwheel's server will also act
as your app server, so requests are correctly forwarded to the http-handler
you define.

To access the ClojureScript REPL:
```clojure
(browser-repl)
```

Running `(browser-repl)` starts the Weasel REPL server, and drops you into a
ClojureScript REPL. Evaluating expressions here will only work once you've
loaded the page, so the browser can connect to Weasel.

When you see the line `Successfully compiled "resources/public/app.js" in 21.36
seconds.`, you're ready to go. Browse to `http://localhost:3449` and enjoy.

To start midje's autotest (for clj tests):
```clojure
(autotest)
```

### Emacs/Cider

Start a repl in the context of your project with `M-x cider-jack-in`.

Switch to repl-buffer with `C-c C-z` and start web and figwheel servers with
`(run)`, and weasel server with `(browser-repl`). Load
[http://localhost:3449](http://localhost:3449) on an external browser, which
connects to weasel, and start evaluating cljs inside Cider.

## Licensing and Ownership

* All code contributed to Camelot will be made available under the same license as Camelot itself (specifically, EPL 1.0 or later).
* The author of a patch owns the copyright of the contributed code; there is no copyright assignment process.

## Questions?

If you have any questions that haven't been answered here, please ask via the Google Group:

https://groups.google.com/forum/#!forum/camelot-project
