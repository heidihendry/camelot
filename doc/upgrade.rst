Upgrading Camelot
-----------------

You have version 1.6.0 - 1.6.5
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As part of upgrading to 1.6.6 or higher, if you were using the new animal detection system it is important to note the storage location the system uses for the progress made by that has changed.

To preserve the progress made after upgrading (see below), please manually move the file named <email_address>-<dataset_name>-detector.edn to live next to the Database folder for that dataset.

The default locations for these files are described here: https://camelot-project.readthedocs.io/en/latest/advanced_config.html.  Please reach out on <a href="https://groups.google.com/forum/#!forum/camelot-project">our Google group</a> if you have any questions about the process.

If the above file is not moved, it is not a problem for Camelot: the animal detection process will recommence from zero.  We're continuing to improve the system, and so this may even find things previously undetected.

Upgrade Process
~~~~~~~~~~~~~~~

# Take a Backup
Take a backup of Camelot's Database directory.  Camelot's database can be backed up by copying the the Database folder while Camelot is not running.  The default location of this folder varies, and is <a href="https://camelot-project.readthedocs.io/en/latest/datasets.html#storage-locations">documented here<a>.


# <a href="https://camelotproject.org">Download the latest version<a>, unzip and put the file contents into your exisiting Camelot folder and start Camelot using the process you usually would.

Camelot will commence an upgrade to its database the first time it is ran, it may mean that it will take a little longer to start up than usual. The time depends on the amount of data, though should usually be a number of seconds and no more than several minutes.

Rollback to a Previous Version
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This is not recommended, but sometimes required. Check with Chris & Heidi in the Google group first.