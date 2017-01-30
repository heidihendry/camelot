Reports
-------

Calculated columns
~~~~~~~~~~~~~~~~~~

Some columns in Camelot are calculated from existing data, and some of
those calculations are worthy of some explanation so that it's clear
what that column represents. Here are the most interesting ones:

Independent observations
^^^^^^^^^^^^^^^^^^^^^^^^

A sighting is considered independent if two photos with the same
sighting are taken at least some time threshold apart. If they are not,
it is considered dependent. Whether they are considered dependent or
independent effects the value of the data in this column.

Photos may be considered dependent if they are within the same Trap
Station Session. That is to say, photos taken by two cameras of a Trap
Station at the same time, if for the same sighting, will be
**dependent**.

Below are some examples showing the various rules of the calculation
(assume T=30 as the threshold). All rules assume sightings are within
the same Trap Station Session; if that were not the case, they would
always be **independent**.

+------------+------------+-------------+--------+-----+
| Sighting   | Quantity   | Lifestage   | Sex    | T   |
+============+============+=============+========+=====+
| Spp. 1     | 1          | Adult       | Male   | 0   |
+------------+------------+-------------+--------+-----+
| Spp. 2     | 1          | Adult       | Male   | 5   |
+------------+------------+-------------+--------+-----+

These are **independent** as it's a different species. The number of
Independent Observations is **2**.

+------------+------------+-------------+--------+------+
| Sighting   | Quantity   | Lifestage   | Sex    | T    |
+============+============+=============+========+======+
| Spp. 1     | 1          | Adult       | Male   | 0    |
+------------+------------+-------------+--------+------+
| Spp. 1     | 1          | Adult       | Male   | 40   |
+------------+------------+-------------+--------+------+

These are **independent** as while it's the same species, it is
separated by T=40. The number of Independent Observations is **2**.

+------------+------------+-------------+--------+-----+
| Sighting   | Quantity   | Lifestage   | Sex    | T   |
+============+============+=============+========+=====+
| Spp. 1     | 1          | Adult       | Male   | 0   |
+------------+------------+-------------+--------+-----+
| Spp. 1     | 2          | Adult       | Male   | 5   |
+------------+------------+-------------+--------+-----+

These are **dependent** as it's the same species, and up to 2 were
sighted within the dependence window. The number of Independent
Observations is **2**.

+------------+------------+-------------+--------+-----+
| Sighting   | Quantity   | Lifestage   | Sex    | T   |
+============+============+=============+========+=====+
| Spp. 1     | 1          | Adult       | Male   | 0   |
+------------+------------+-------------+--------+-----+
| Spp. 1     | 1          | Juvenile    | Male   | 5   |
+------------+------------+-------------+--------+-----+

These are **independent** as while it's the same species, one is a
juvenile and the other an adult. The number of Independent Observations
is **2**.

+------------+------------+----------------+----------------+-----+
| Sighting   | Quantity   | Lifestage      | Sex            | T   |
+============+============+================+================+=====+
| Spp. 1     | 1          | Adult          | Male           | 0   |
+------------+------------+----------------+----------------+-----+
| Spp. 1     | 1          | Unidentified   | Unidentified   | 5   |
+------------+------------+----------------+----------------+-----+

These are **dependent** as while the lifestage and sex are not the same,
unidentified values are *inferred*. The number of Independent
Observations is **1**.

+------------+------------+----------------+----------------+------+
| Sighting   | Quantity   | Lifestage      | Sex            | T    |
+============+============+================+================+======+
| Spp. 1     | 1          | Unidentified   | Unidentified   | 0    |
+------------+------------+----------------+----------------+------+
| Spp. 1     | 1          | Adult          | Male           | 5    |
+------------+------------+----------------+----------------+------+
| Spp. 1     | 1          | Unidentified   | Female         | 10   |
+------------+------------+----------------+----------------+------+

Sighting 2 of Spp. 1 is **dependent** on sighting 1, due to inference on
Lifestage and Sex. Sighting 3 is **independent** of both sighting 1 and
sighting 2 due to that inference. The number of independent observations
is **2**.

The value of the threshold is defined in Camelot's settings menu
("Independent Sighting Threshold (mins)").

Nocturnal (%)
~~~~~~~~~~~~~

This is simply the number of photos taken at night, divided by the
number of photos. The interesting part is what is considered to be "at
night".

Night is determined as a time after sunset and before sunrise, given a
particular set of GPS coordinates and on a particular day using the
sunrise and sunset times as calculated by |alamac_link|.

This algorithm does not attempt to account for atmospheric or
geographical features, though will typically be accurate to within
several minutes of the actual sunrise and sunset times.

.. |alamac_link| raw:: html

   <a href="http://williams.best.vwh.net/sunrise_sunset_algorithm.htm" target="_blank">an algorithm published by the Nautical Almanac Office</a>

Abundance Index
~~~~~~~~~~~~~~~

The Abundance Index is calculated using two pieces of data: the number
of number of independent observations, and the number of nights of
elapsed operation for a camera trap session, or for the combined elapsed
time of all camera trap sessions (depending on the report).

The calculation of this value is then:

``100 * Independent Observations / Nights``
