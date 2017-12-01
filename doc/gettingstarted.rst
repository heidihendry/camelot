Getting started
---------------

Prerequisites
~~~~~~~~~~~~~

Java Runtime
^^^^^^^^^^^^

Camelot requires Java 8u91 (on later) to be installed on the system it
will run on before it can be used.

Java can be downloaded here:

|jre_link|

.. |jre_link| raw:: html

   <a href="http://www.oracle.com/technetwork/java/javase/downloads/index.html" target="_blank">http://www.oracle.com/technetwork/java/javase/downloads/index.html</a>

If using OSX, you will need to install the "JDK". For Windows and Linux,
you can install either the "JRE" or the "JDK".

Web browser
^^^^^^^^^^^

Camelot supports the latest versions of the following browsers:

-  Chrome
-  Firefox
-  Edge

Installation
~~~~~~~~~~~~

Download the `latest version of
Camelot <https://s3-ap-southeast-2.amazonaws.com/camelot-project/release/camelot-1.3.6.zip>`__.

Unzip the archive. To run Camelot:

**Windows**: Double click ``camelot-desktop.bat``

**OSX**: Double click ``camelot-desktop.command``

**Linux**: Double click ``camelot-desktop.sh``

After 10 seconds, Camelot should appear in a new tab in your web
browser. If Camelot doesn't open automatically, you can access it via
your web browser by browsing to:

::

    http://localhost:5341/

If running Camelot on a server, you can instead use:

::

    java -jar /path/to/camelot-<version>.jar -server

See the section on 'Networked usage' for more information.
