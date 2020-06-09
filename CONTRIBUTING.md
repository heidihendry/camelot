## Contribution Guide

Welcome to the Camelot project!  Our goal is to provide an intuitive yet powerful platform for wildlife conservation research using camera trapping.  There's a whole variety of skills needed to achieve this and your skills would be very much appreciated.

## How can I contribute?

Here's some specific ways you can help:

### Raising issues for bugs and problems

[Raise an issue](https://gitlab.com/camelot-project/camelot/issues) should you encounter a problem or limitation within Camelot.  When reporting a problem, please give the issue a clear and meaningful subject and try to provide as much information about the problem itself as possible.

### Requesting features

[Raise an feature request](https://tree.taiga.io/project/cshclm-camelot/issues) to suggest a change in Camelot.

When raising a suggestion, give it a clear and meaningful title.  For the description, consider using the following template:

```
As a [a user role]
I would like [the goal of the feature]
So that [the benefit of the feature to that user]
```

When raising a suggestion, remember the best feature requests clearly capture the problem and the value of solving that problem.

### Documentation improvements

Regardless of how good a piece of software is, it offers no value if it can't be understood.  We want to provide excellent documentation, so if you see a mistake, omission or area it can be improved, please do let us know.

If you're comfortable with git, we gratefully accept [Merge Requests](https://docs.gitlab.com/ce/gitlab-basics/add-merge-request.html) for documentation improvements.  Otherwise please [Raise an issue](https://gitlab.com/camelot-project/camelot/issues) describing the documentation change you would like to be included.

### Translations

Currently Camelot has internal support for translations, but has not been translated.  If you know English and another language, translations would be greatly appreciated.

The Camelot project has two different types of resources available for translation:

1. Camelot itself

Building a translation simply involves taking the strings in the [English translation file](https://gitlab.com/camelot-project/camelot/blob/master/src/cljc/camelot/translation/en.cljc) and replacing them with translated strings.  Occasionally strings will contain `%s` or similar; this is a placeholder which gets replaced with other content by Camelot.

2. Project documentation.

Translations for project files, such as the User Guide or this Contribution Guide, are help to make Camelot more accessible to native speakers of other languages.

To contribute new translations or improvements to existing translations back to the project, please either raise a [merge request](https://gitlab.com/camelot-project/camelot/merge_requests) or [raise an issue](https://gitlab.com/camelot-project/camelot/issues).

### Code contributions

And of course we accept code contributions. :-)

If you're looking for something to work on, check to see if there are any [outstanding issues](https://gitlab.com/camelot-project/camelot/issues) or take a look [through our backlog](https://tree.taiga.io/project/cshclm-camelot/backlog).  If you have "scratched your own itch", please do open a [merge request](https://gitlab.com/camelot-project/camelot/merge_requests), as it would be great if we could include your changes in the project.

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

1. Camelot uses [Leiningen](https://leiningen.org/) for managing the project build.  First, follow the [installation instructions](https://leiningen.org/#install) for Leiningen.

2. Once installed, building Camelot is one command\*\*:

```
lein uberjar
```

This will build Camelot and produce a new .jar file, which can be ran with:

```
java -jar target/camelot.jar
```

If you encounter problems, raise an issue or reach out via the Google Group.

\* Camelot is designed to be developed within a \*nix environment (e.g., Linux or OSX).  It may be possible to follow this process within Windows using tools such as WSL.

\*\* Building and running artifacts probably shouldn't be your normal dev-loop.  Most Clojure developers prefer to work interactively at a REPL.

### Running tests

Camelot currently has 2 test suites, one for clj, and one for cljs. To run the tests use:

```
./script/test
```

### Emacs/Cider

Start a repl in the context of your project with `M-x cider-jack-in`.

Switch to repl-buffer with `C-c C-z` and start the development environment by
typing `(go)` at the prompt, After a minute you'll see that Camelot has
started up.  Load [http://localhost:5341](http://localhost:5341) in an
external browser and you're all set. Happy hacking.

## Licensing and Ownership

* All code contributed to Camelot will be made available under the same license as Camelot itself (specifically, AGPL v3 or later).
* New code contributors will need confirm that they agree to transfer copyright ownership of their contribution.

## Questions?

If you have any questions that haven't been answered here, please ask via the Google Group:

https://groups.google.com/forum/#!forum/camelot-project
