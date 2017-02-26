Working with large datasets
---------------------------

*This section applies to versions of Camelot 1.2.0 and beyond.*

You'll have noticed in setting up Camelot, that you probably did not need to
provision servers, configure relational databases, network equipment and so
on. Camelot does *not* treat working with massive volumes of data as its
priority use case. However, we've spent much time ensuring you won't encounter
growth pain, no matter how much data you want to throw at it (within reason,
of course).

This page describes what you can expect, and recommendations should you be
dealing with large datasets.

Limitations
~~~~~~~~~~~

Camelot has no known upper limit on the amount of data it can support, however
some parts of Camelot will take longer to load as the size of the dataset
grows. The authors have simulated datasets with 2 million images to ensure
that Camelot will perform well for 99% of datasets Camelot could be used for.

The main performance considerations with this volume of data is performing the
initial load of the library, searching the library, and the CPU and memory
constraints required to produce reports.

To serve as a guide for how Camelot may perform for large datasets, below is
the approximate times to produce those using a high-end laptop in 2017 (Dell
Precision 5510; 16GB RAM; Intel i7-6820HQ; SSD) with a maximum JVM heap size
of 14GB:

* **Full Export report**: 368 seconds
* **Library load time**: <2 seconds
* **Library search time (basic search)**: <2 seconds
* **Library search time (full/advanced search)**: 50 seconds

Considerations for your dataset
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This section will describe considerations relevant for using Camelot with
larger datasets on a Camelot server, and offer some guidelines for what the
authors would expect to be reasonable configurations under various scenarios.

Data for Camelot's performance is known for only a small number of scenarios,
thus the numbers offered are very much guidelines and you may find different
considerations important in your use case.

Storage
^^^^^^^

High throughput and low latency storage for a database is always nice to have,
though should not be strictly necessary to use Camelot with large datasets.
However, using Camelot with remote storage, for example, running Camelot on a
laptop and connecting to a database stored on a NAS over wifi, is unlikely to
result in nice performance characteristics.  Effort should be made to reduce
the latency and maximise throughput between where Camelot is running, and
where its database resides.

That said, much of the volume of data for an installation of Camelot is
occupied by images.  Images may reside on lower-throughput and higher latency
storage as it does not impact significantly on Camelot's performance profile.
A mechanism for storing the ``Database`` and ``Media`` directories on separate
filesystems is not provided by Camelot directly.  It is however possible on
most Operating Systems with some technical trickery.

Memory
^^^^^^

*This ignores restrictions on addressable memory imposed by 32-bit operating systems. The authors recommend ensuring you are NOT using a 32-bit operating system on the machine Camelot runs on.*

Memory is a very difficult consideration to judge. Generally speaking, while
there is sufficient memory, it has little impact upon performance. However
when there is not enough memory, it may result in things which will take
considerably longer to complete, or may never complete.  There are two main
aspects to memory in Camelot: the physical memory available, and the memory
available to the JVM heap.

The most important consideration is maximum size of the JVM heap.  Regardless
of how much physical memory is available to a machine, Camelot may only use
the memory available in the JVM heap (and some small additional amounts, as
per other JVM configurations), and this is something must be configured
manually for large datasets.

The JVM heap size should not exceed the size of physical memory available, and
ideally should not impinge upon the resources required by other applications
on the machine Camelot is running upon.

There are a number of ways to set the JVM Heap size. We recommend setting it
via the command line (which may be in a script for convenience and
consistency).  The invocation would look as follows:

.. code:: shell

  java -Xms6g -Xmx6g -server -jar path/to/camelot.jar

The above will set the initial heap size (``Xms``) and the maximum heap size
(``Xmx``) to 6GB. It will also ensure the Java Hotspot is tuned for server
workloads via the ``-server`` flag.

As a (very) rough guide, the authors suggest an additional 600MB of heap space
for every 100,000 images, with a starting heap space of 2GB.  You needn't be
concerned with setting the initial or maximum heap size to 2GB explicitly, as
this will be the default on most machines.  That is to say, you probably
needn't be concerned with configuring the heap space if your dataset is
smaller than 100,000 images.

Concurrent users
^^^^^^^^^^^^^^^^

We currently do not have sufficient data on how the number of concurrent users
impacts upon Camelot's performance in Real World usage.  It depends heavily on
the usage of those users.  Should you be using Camelot with a large number of
users, and have any questions or feedback about your particular use case,
please do reach out via the |group_link|.

.. |group_link| raw:: html

   <a href="https://groups.google.com/forum/#!forum/camelot-project" target="_blank">forum</a>

Client machines
~~~~~~~~~~~~~~~

This section applies to client machines: those connecting to a Camelot server,
which do not run a copy of Camelot themselves.

Generally speaking, any computer able to achieve an acceptable degree of
responsiveness should be a fine candidate for accessing Camelot running on a
remote machine.  The main consideration of client machines is less-so
performance, than it is screen resolution.  In common usage, there should be
no discernible degradation on performance for large datasets for client
machines.
