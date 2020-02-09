Configuration Internals
-----------------------

This setting describes the internals of Camelot's data storage locations and
configuration files. This is intended as a resource for very specialised use
cases and for IT professionals.

Camelot has two directories: one for configuration, and one for data
storage. The location of these directories depends on the OS.

Data locations
~~~~~~~~~~~~~~

By default, Camelot stores all data under the current user's home folder for
the operating system.

Data
^^^^

-  **Windows**: %LOCALAPPDATA%\\camelot
-  **MacOS**: $HOME/Library/Application Support/camelot
-  **Linux**: $HOME/.local/share/camelot

Configuration
^^^^^^^^^^^^^

-  **Windows**: %APPDATA%\\camelot
-  **MacOS**: $HOME/Library/Preferences/camelot
-  **Linux**: $HOME/.config/camelot

Data directory
~~~~~~~~~~~~~~

The data directory will, by default, contain three subdirectories:
``Database``, ``Media`` and ``FileStore``. Database is an Apache Derby
database.  Imported media is not stored in the database, but in the ``Media``
folder. Finally, the ``FileStore`` contains files for the Survey's "Related
files" feature.

A custom data directory can be set using the ``CAMELOT_DATADIR`` environment
variable. The Database and Media directories will be created (if necessary)
and stored within that nominated directory. If ``CAMELOT_DATADIR`` is not set,
Camelot will fall-back to using the standard locations (as above).  The usage
of ``CAMELOT_DATADIR`` can be selectively overridden by specifying paths to
alternate directories, as described below.

Each of the ``Database``, ``Media`` and ``FileStore`` directories should
be backed up routinely.

Config directory
~~~~~~~~~~~~~~~~

config.json
^^^^^^^^^^^

``config.json`` is the global camelot configuration file. Some values in this
file can be set via the administration interface in Camelot. Care should be
taken if editing this file manually.

``config.json`` supports the following properties:

* ``paths``: describes the various data paths to be used by Camelot. The following paths are supported:

  * ``media``: the folder path to the directory where media is to be stored.
  * ``database``: the base to the folder which is to *contain* a ``Database`` directory.
  * ``filestore-base``: the folder path to where Camelot should keep its uploaded survey files.
  * ``backup``: the folder path to where Camelot stores backups.
  * ``log``: the folder path to where Camelot should store its logs.
  * ``application``: the folder path to where Camelot can find a Camelot .jar file.
  * ``root``: the root folder which Camelot can search for external data. For example, for scanning for bulk import

* ``server``: an object describing Camelot server-specific configuration

  * ``http-port``: a number describing the port to run Camelot's HTTP server on (default: 5341)
  * ``media-importers``: the number of concurrent threads for importing media (default: 4)
  * ``max-heap-size``: a number representing the desired maximum heap size (in MB) for the Camelot server JVM (default: <blank>)
  * ``jvm-extra-args``: additional arguments you wish to pass to ``java`` when starting the Camelot server. (default: <blank>)

* ``detector``: an object describing Camelot wildlife detection specific configuration

  * ``enabled``: a boolean representing whether the feature is enabled
  * ``username``: the username registered for the service
  * ``password``: the password belonging to that registered user
  * ``confidence-threshold``: a number between `0` and `1` indicating a lower-bound for the confidence level that a suggestion is correct (default: 0.9)

* ``java-command``: the path to the ``java`` executable on this system (default: ``java``)
* ``open-browser-on-startup``: whether the Camelot administration software should automatically view Camelot after startup (default: ``true``)
* ``send-usage-data``: whether to publish anonymous usage data to the Camelot team for helping to improve the software (default: ``false``)
* ``species-name-style``: the style to show species names in the Camelot user interface; options are ``scientific`` or ``common-name`` (default: ``scientific``)

All of the above properties are optional and default values will be used where not specified.

In practice, a simple ``config.json`` might look as follows:

.. code-block:: json

   {
     "paths": {
       "media": "/path/to/ext-hdd/Media"
     }
   }
