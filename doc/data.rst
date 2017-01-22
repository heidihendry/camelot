Fields and data
---------------

This section describes the data within Camelot.

Data Dictionary
~~~~~~~~~~~~~~~

+-----------+---------------+--------------+-------------+--------------+
| Field     | Description   | Datatype     | Required?   | Importable?  |
+===========+===============+==============+=============+==============+
| survey-id | Unique ID of  | Integer      | Yes         | No           |
|           | the survey    |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| survey-na | (Unique) name | String       | Yes         | No           |
| me        | for the       |              |             |              |
|           | survey        |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| survey-no | Survey        | String       | Yes         | No           |
| tes       | description   |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| site-id   | Unique ID of  | Integer      | Yes         | No           |
|           | the site      |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| site-name | Name for the  | String       | Yes         | Yes          |
|           | site          |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| site-subl | Name by which | String       | No          | Yes          |
| ocation   | the area is   |              |             |              |
|           | known         |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| site-city | City or       | String       | No          | Yes          |
|           | nearest city  |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| site-stat | State/Provinc | String       | No          | Yes          |
| e-provinc | e             |              |             |              |
| e         | of the site   |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| site-coun | Country or    | String       | No          | Yes          |
| try       | nearest       |              |             |              |
|           | country to    |              |             |              |
|           | the site      |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| site-area | Approximate   | Decimal      | No          | Yes          |
|           | area covered  |              |             |              |
|           | by the site,  |              |             |              |
|           | in km^2       |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| site-note | Site          | String       | Yes         | No           |
| s         | description   |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| survey-si | Unique ID of  | Integer      | Yes         | No           |
| te-id     | the           |              |             |              |
|           | association   |              |             |              |
|           | between a     |              |             |              |
|           | survey a      |              |             |              |
|           | site.         |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| trap-stat | Unique ID of  | Integer      | Yes         | No           |
| ion-id    | the Trap      |              |             |              |
|           | Station       |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| trap-stat | Name of the   | String       | Yes         | Yes          |
| ion-name  | Trap Station  |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| trap-stat | Latitude of   | Decimal (6   | Yes         | Yes          |
| ion-latit | the trap      | d.p.         |             |              |
| ude       | station, as a | precision)   |             |              |
|           | decimal       |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| trap-stat | Longitude of  | Decimal (6   | Yes         | Yes          |
| ion-longi | the trap      | d.p.         |             |              |
| tude      | station, as a | precision)   |             |              |
|           | decimal       |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| trap-stat | Altitude of   | Integer      | No          | Yes          |
| ion-altit | the trap      |              |             |              |
| ude       | station, in   |              |             |              |
|           | meters        |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| trap-stat | Unique ID of  | Integer      | Yes         | No           |
| ion-sessi | the session   |              |             |              |
| on-id     |               |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| trap-stat | Start of the  | Timestamp    | Yes         | Yes          |
| ion-sessi | session       |              |             |              |
| on-start- |               |              |             |              |
| date      |               |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| trap-stat | End of the    | Timestamp    | Yes         | Yes          |
| ion-sessi | session       |              |             |              |
| on-end-da |               |              |             |              |
| te        |               |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| trap-stat | Unique ID of  | Integer      | Yes         | No           |
| ion-sessi | the camera    |              |             |              |
| on-camera | for a session |              |             |              |
| -id       |               |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| camera-id | Unique ID of  | Integer      | Yes         | No           |
|           | the camera    |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| camera-na | (Unique) name | String       | Yes         | Yes          |
| me        | of the camera |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| camera-ma | Make of the   | String       | No          | Yes          |
| ke        | camera        |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| camera-mo | Model of the  | String       | No          | Yes          |
| del       | camera        |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| camera-no | Description   | String       | No          | Yes          |
| tes       | of the camera |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| media-id  | Unique ID of  | Integer      | Yes         | No           |
|           | the media     |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| media-cap | Time the      | Timestamp    | Yes         | Yes          |
| ture-time | image was     |              |             |              |
| stamp     | captured      |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| media-cam | Flag          | Boolean      | No          | Yes          |
| eracheck  | indicating    |              |             |              |
|           | whether the   |              |             |              |
|           | media was a   |              |             |              |
|           | camera check  |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| media-pro | Flag          | Boolean      | No          | Yes          |
| cessed    | indicating    |              |             |              |
|           | whether       |              |             |              |
|           | processing of |              |             |              |
|           | the media is  |              |             |              |
|           | complete      |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| media-att | Flag          | Boolean      | No          | Yes          |
| ention-ne | indicating    |              |             |              |
| eded      | whether the   |              |             |              |
|           | media has     |              |             |              |
|           | been checked  |              |             |              |
|           | and needs     |              |             |              |
|           | further       |              |             |              |
|           | attention     |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| media-fil | Base filename | String       | Yes         | No           |
| ename     | for the media |              |             |              |
|           | in Camelot    |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| media-for | File format   | String       | Yes         | No           |
| mat       | of the        |              |             |              |
|           | original      |              |             |              |
|           | media         |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| media-not | Notes about   | String       | No          | No           |
| es        | the media     |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| sighting- | Unique ID of  | Integer      | Yes         | No           |
| id        | the sighting  |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| sighting- | Number of a   | Integer      | Yes         | Yes          |
| quantity  | species in    |              |             |              |
|           | the media     |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| sighting- | Sex of the    | String       | No          | Yes          |
| sex       | species       | ('M'/'F'/'un |             |              |
|           |               | identified') |             |              |
+-----------+---------------+--------------+-------------+--------------+
| sighting- | Life-stage of | String       | No          | Yes          |
| lifestage | the species   | ('Adult'/'Ju |             |              |
|           |               | venile'/'uni |             |              |
|           |               | dentified')  |             |              |
+-----------+---------------+--------------+-------------+--------------+
| taxonomy- | Unique ID of  | Integer      | Yes         | No           |
| id        | a species     |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| taxonomy- | Species name  | String       | Yes         | Yes          |
| species   |               |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| taxonomy- | Genus name    | String       | Yes         | Yes          |
| genus     |               |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| taxonomy- | Family name   | String       | No          | Yes          |
| family    |               |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| taxonomy- | Order name    | String       | No          | Yes          |
| order     |               |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| taxonomy- | Class name    | String       | No          | Yes          |
| class     |               |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| taxonomy- | Common name   | String       | Yes         | Yes          |
| common-na | of the        |              |             |              |
| me        | species       |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| taxonomy- | Notes about   | String       | No          | Yes          |
| notes     | the species   |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| species-m | ID of the     | Integer      | Yes         | No           |
| ass-id    | species mass  |              |             |              |
|           | bracket       |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| species-m | Start of the  | Decimal      | Yes         | No           |
| ass-start | mass bracket  |              |             |              |
|           | (kg)          |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| species-m | End of the    | Decimal      | Yes         | No           |
| ass-end   | mass bracket  |              |             |              |
|           | (kg)          |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| photo-id  | Unique ID of  | Integer      | Yes         | No           |
|           | the photo     |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| photo-fnu | F-stop        | String       | No          | Yes          |
| mber-sett | setting when  |              |             |              |
| ing       | the photo was |              |             |              |
|           | taken         |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| photo-exp | Exposure      | String       | No          | Yes          |
| osure-val | setting when  |              |             |              |
| ue        | the photo was |              |             |              |
|           | taken         |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| photo-fla | Whether the   | String       | No          | Yes          |
| sh-settin | flash was     |              |             |              |
| g         | triggered     |              |             |              |
|           | when taking   |              |             |              |
|           | the photo     |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| photo-foc | The focal     | String       | No          | Yes          |
| al-length | length when   |              |             |              |
|           | the photo was |              |             |              |
|           | taken         |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| photo-iso | The ISO       | String       | No          | Yes          |
| -setting  | setting when  |              |             |              |
|           | the photo was |              |             |              |
|           | taken         |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| photo-ori | The camera    | String       | No          | Yes          |
| entation  | orientation   |              |             |              |
|           | when the      |              |             |              |
|           | photo was     |              |             |              |
|           | taken         |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| photo-res | The width of  | Integer      | No          | Yes          |
| olution-x | the image, in |              |             |              |
|           | pixels        |              |             |              |
+-----------+---------------+--------------+-------------+--------------+
| photo-res | The height of | Integer      | No          | Yes          |
| olution-y | the image, in |              |             |              |
|           | pixels        |              |             |              |
+-----------+---------------+--------------+-------------+--------------+

Bulk Import default mapping column
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

By using the Default Column Names in a CSV for a Bulk Import, Camelot
will automatically establish the mapping to the correct field.

+-----------------------------------+---------------------------------+
| Field                             | Default Column Name             |
+===================================+=================================+
| camera-make                       | Make                            |
+-----------------------------------+---------------------------------+
| camera-model                      | Model                           |
+-----------------------------------+---------------------------------+
| camera-name                       | Camera Name                     |
+-----------------------------------+---------------------------------+
| camera-notes                      | Camera Notes                    |
+-----------------------------------+---------------------------------+
| media-attention-needed            | Attention Needed Flag           |
+-----------------------------------+---------------------------------+
| media-cameracheck                 | Camera Check Flag               |
+-----------------------------------+---------------------------------+
| media-capture-timestamp           | Date/Time                       |
+-----------------------------------+---------------------------------+
| media-processed                   | Media Processed Flag            |
+-----------------------------------+---------------------------------+
| photo-exposure-value              | Exposure Bias Value             |
+-----------------------------------+---------------------------------+
| photo-flash-setting               | Flash                           |
+-----------------------------------+---------------------------------+
| photo-fnumber-setting             | Aperture Value                  |
+-----------------------------------+---------------------------------+
| photo-focal-length                | Focal Length                    |
+-----------------------------------+---------------------------------+
| photo-iso-setting                 | ISO Speed Ratings               |
+-----------------------------------+---------------------------------+
| photo-orientation                 | Orientation                     |
+-----------------------------------+---------------------------------+
| photo-resolution-x                | Image Height                    |
+-----------------------------------+---------------------------------+
| photo-resolution-y                | Image Width                     |
+-----------------------------------+---------------------------------+
| sighting-lifestage                | Sighting Life Stage             |
+-----------------------------------+---------------------------------+
| sighting-quantity                 | Sighting Quantity               |
+-----------------------------------+---------------------------------+
| sighting-sex                      | Sighting Sex                    |
+-----------------------------------+---------------------------------+
| site-area                         | Site Area (km2)                 |
+-----------------------------------+---------------------------------+
| site-city                         | City                            |
+-----------------------------------+---------------------------------+
| site-country                      | Country/Primary Location Name   |
+-----------------------------------+---------------------------------+
| site-name                         | Site Name                       |
+-----------------------------------+---------------------------------+
| site-notes                        | Site Notes                      |
+-----------------------------------+---------------------------------+
| site-state-province               | Province/State                  |
+-----------------------------------+---------------------------------+
| site-sublocation                  | Sub-location                    |
+-----------------------------------+---------------------------------+
| taxonomy-class                    | Class                           |
+-----------------------------------+---------------------------------+
| taxonomy-common-name              | Species Common Name             |
+-----------------------------------+---------------------------------+
| taxonomy-family                   | Family                          |
+-----------------------------------+---------------------------------+
| taxonomy-genus                    | Genus                           |
+-----------------------------------+---------------------------------+
| taxonomy-notes                    | Species Notes                   |
+-----------------------------------+---------------------------------+
| taxonomy-order                    | Order                           |
+-----------------------------------+---------------------------------+
| taxonomy-species                  | Species                         |
+-----------------------------------+---------------------------------+
| trap-station-altitude             | GPS Altitude                    |
+-----------------------------------+---------------------------------+
| trap-station-latitude             | Camelot GPS Latitude            |
+-----------------------------------+---------------------------------+
| trap-station-longitude            | Camelot GPS Longitude           |
+-----------------------------------+---------------------------------+
| trap-station-name                 | Trap Station Name               |
+-----------------------------------+---------------------------------+
| trap-station-notes                | Trap Station Notes              |
+-----------------------------------+---------------------------------+
| trap-station-session-end-date     | Session End Date                |
+-----------------------------------+---------------------------------+
| trap-station-session-start-date   | Session Start Date              |
+-----------------------------------+---------------------------------+

Library filter field shorthands
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The library supports filtering on the vast majority of fields in
Camelot. Some of these fields are more useful for filtering on than
others and so have shorthands. Below is each of the shorthands and the
field they are associated with.

+---------------------+---------------------------+
| Shorthand           | Associated Field          |
+=====================+===========================+
| camera              | camera-name               |
+---------------------+---------------------------+
| city                | site-city                 |
+---------------------+---------------------------+
| class               | taxonomy-class            |
+---------------------+---------------------------+
| common              | taxonomy-common-name      |
+---------------------+---------------------------+
| family              | taxonomy-family           |
+---------------------+---------------------------+
| flagged             | media-attention-needed    |
+---------------------+---------------------------+
| genus               | taxonomy-genus            |
+---------------------+---------------------------+
| lat                 | trap-station-latitude     |
+---------------------+---------------------------+
| loc                 | site-sublocation          |
+---------------------+---------------------------+
| long                | trap-station-longitude    |
+---------------------+---------------------------+
| make                | camera-make               |
+---------------------+---------------------------+
| model               | camera-model              |
+---------------------+---------------------------+
| order               | taxonomy-order            |
+---------------------+---------------------------+
| processed           | media-processed           |
+---------------------+---------------------------+
| reference-quality   | media-reference-quality   |
+---------------------+---------------------------+
| site                | site-name                 |
+---------------------+---------------------------+
| species             | taxonomy-label            |
+---------------------+---------------------------+
| testfire            | media-cameracheck         |
+---------------------+---------------------------+
| trap                | trap-station-name         |
+---------------------+---------------------------+
| trapid              | trap-station-id           |
+---------------------+---------------------------+
