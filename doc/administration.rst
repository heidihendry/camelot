Administration and advanced configuration
-----------------------------------------

*This section is intended for people with strong IT knowledge.*

Camelot has two directories: one for configuration, and one for data
storage. The location of these directories depends on the OS.

Locations
~~~~~~~~~

By default, Camelot stores all data under the current user's home folder for
the operating system.

Windows
^^^^^^^

-  **Data**: %LOCALAPPDATA%\\camelot
-  **Config**: %APPDATA%\\camelot

(Entering, for example, ``%LOCALAPPDATA%``, as a folder path may seem unusual,
though is a valid path under Windows.)

OSX
^^^

-  **Data**: $HOME/Library/Application Support/camelot
-  **Config**: $HOME/Library/Preferences/camelot

Linux
^^^^^

-  **Data**: $HOME/.local/share/camelot
-  **Config**: $HOME/.config/camelot

Data Directory
~~~~~~~~~~~~~~

The data directory will contain three subdirectories: ``Database``,
``Media`` and ``FileStore``. Database is an Apache Derby database.
Imported media is not stored in the database, but in the ``Media``
folder. Finally, the ``FileStore`` contains files for the Survey's
"Related files" feature.

A custom data directory can be set using the ``CAMELOT_DATADIR``
environment variable. The Database and Media directories will be created
(if necessary) and stored within that nominated directory. If
``CAMELOT_DATADIR`` is not set, Camelot will fall-back to using the
standard locations (as above).

Each of the ``Database``, ``Media`` and ``FileStore`` directories should
be backed up routinely.

Storing Media and Database separately
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It can be desirable to store the ``Media`` directory separately from the
``Database`` directory.  For example, for many GB of images it may be
desirable to place the images themselves on cheaper storage such as a HDD,
while keeping the ``Database`` directory on an SSD for fast access.

This can be achieved with Camelot using features provided by the Operating
System.  The process is as follows:

1. set the datadir to the location you want for your database on the SSD via
   the CAMELOT_DATADIR environment variable.
2. move the Media (and optionally FileStore) folder(s) to the locations you
   want on the HDD.
3. finally create |linux_symlink| (Linux, MacOS) or |windows_junctions|
   (Windows) for the moved folders from the datadir to the the location you
   moved them, ensuring the links are named identically to the original
   folders in datadir (i.e., ``Media`` and ``FileStore``).

When choosing the locations keep in mind that this data isn't intended to be
modified outside of Camelot, and thus it is recommended to ensure appropriate
permissions are set and not to place these inside shared folders.

.. |linux_symlink| raw:: html

   <a href="https://kb.iu.edu/d/abbe" target="_blank">symlinks</a>

.. |windows_junctions| raw:: html

   <a href="https://docs.microsoft.com/en-us/sysinternals/downloads/junction" target="_blank">junctions</a>

Config Directory
~~~~~~~~~~~~~~~~

config.clj
^^^^^^^^^^

``config.clj`` is the global camelot configuration file. Some values in
this file can be set via the Settings menu in Camelot, while others may
be internal or legacy settings. Care should be taken if editing this
file manually.\ ``config.clj`` is the global camelot configuration file.
All configuration values available in this can also be set through the
settings panel in the UI.

Custom Reports
~~~~~~~~~~~~~~

Custom reports and column definitions for reports can be registered by
creating a *reports module*. A reports module can also override existing
reports and columns.

Reports modules are Clojure files (``.clj`` extension) and are stored
under the ``modules`` subdirectory of Camelot's config directory
(described above).

All modules in this directory will be loaded before each report is ran.

Here's an example module to create and register a custom column, and a
custom report using that column.

.. code:: clojure

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

Camelot will treat your field differently when it comes to generating
the report, depending on how it the field is named.

-  Fields ending in "-id" are converted to Java Longs.
-  Fields ending in "-date" are converted to Joda Dates.
-  Fields ending in "-float" are converted to Java Floats.
-  Fields ending in "-num" are converted to a suitable type. Check the
   ``edn/read-string`` documentation for details.

For more module examples, check out Camelot's |builtin_link|.

.. |builtin_link| raw:: html

   <a href="https://gitlab.com/camelot-project/camelot/tree/master/src/clj/camelot/report/module/builtin/?at=master" target="_blank">built-in reports and columns</a>
