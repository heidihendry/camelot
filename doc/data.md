## Fields and data

This page describes the data within Camelot.

### Data Dictionary

| Field    | Description  | Datatype    | Required?  | Importable? |
| :------- | :----------- | :---------- | :--------- | :---------- |
| survey-id | Unique ID of the survey | Integer  | Yes | No |
| survey-name | (Unique) name for the survey | String  | Yes | No |
| survey-notes | Survey description | String  | Yes | No |
| site-id | Unique ID of the site | Integer  | Yes | No |
| site-name | Name for the site | String  | Yes | Yes |
| site-sublocation | Name by which the area is known | String  | No | Yes |
| site-city | City or nearest city | String  | No | Yes |
| site-state-province | State/Province of the site | String  | No | Yes |
| site-country | Country or nearest country to the site | String | No | Yes |
| site-area | Approximate area covered by the site, in km^2 | Decimal | No | Yes |
| site-notes | Site description | String  | Yes | No |
| survey-site-id | Unique ID of the association between a survey a site. | Integer | Yes | No |
| trap-station-id | Unique ID of the Trap Station | Integer | Yes | No |
| trap-station-name | Name of the Trap Station | String | Yes | Yes |
| trap-station-latitude | Latitude of the trap station, as a decimal | Decimal (6 d.p. precision) | Yes | Yes |
| trap-station-longitude | Longitude of the trap station, as a decimal | Decimal (6 d.p. precision) | Yes | Yes |
| trap-station-altitude | Altitude of the trap station, in meters | Integer | No | Yes |
| trap-station-session-id | Unique ID of the session | Integer | Yes | No |
| trap-station-session-start-date | Start of the session | Timestamp | Yes | Yes |
| trap-station-session-end-date | End of the session | Timestamp | Yes | Yes |
| trap-station-session-camera-id | Unique ID of the camera for a session | Integer | Yes | No |
| camera-id | Unique ID of the camera | Integer | Yes | No |
| camera-name | (Unique) name of the camera | String | Yes | Yes |
| camera-make | Make of the camera | String | No | Yes |
| camera-model | Model of the camera | String | No | Yes |
| camera-notes | Description of the camera | String | No | Yes |
| media-id | Unique ID of the media | Integer | Yes | No |
| media-capture-timestamp | Time the image was captured | Timestamp | Yes | Yes |
| media-cameracheck | Flag indicating whether the media was a camera check | Boolean | No | Yes |
| media-processed | Flag indicating whether processing of the media is complete | Boolean | No | Yes |
| media-attention-needed | Flag indicating whether the media has been checked and needs further attention | Boolean | No | Yes |
| media-filename | Base filename for the media in Camelot | String | Yes | No |
| media-format | File format of the original media | String | Yes | No |
| media-notes | Notes about the media | String | No | No |
| sighting-id | Unique ID of the sighting | Integer | Yes | No |
| sighting-quantity | Number of a species in the media | Integer | Yes | Yes |
| sighting-sex | Sex of the species | String ('M'/'F'/'unidentified') | No | Yes |
| sighting-lifestage | Life-stage of the species | String ('Adult'/'Juvenile'/'unidentified') | No | Yes |
| taxonomy-id | Unique ID of a species | Integer | Yes | No |
| taxonomy-species | Species name | String | Yes | Yes |
| taxonomy-genus | Genus name | String | Yes | Yes |
| taxonomy-family | Family name | String | No | Yes |
| taxonomy-order | Order name | String | No | Yes |
| taxonomy-class | Class name | String | No | Yes |
| taxonomy-common-name | Common name of the species | String | Yes | Yes |
| taxonomy-notes | Notes about the species | String | No | Yes |
| species-mass-id | ID of the species mass bracket | Integer | Yes | No |
| species-mass-start | Start of the mass bracket (kg) | Decimal | Yes | No |
| species-mass-end | End of the mass bracket (kg) | Decimal | Yes | No |
| photo-id | Unique ID of the photo | Integer | Yes | No |
| photo-fnumber-setting | F-stop setting when the photo was taken | String | No | Yes |
| photo-exposure-value | Exposure setting when the photo was taken | String | No | Yes |
| photo-flash-setting | Whether the flash was triggered when taking the photo | String | No | Yes |
| photo-focal-length | The focal length when the photo was taken | String | No | Yes |
| photo-iso-setting | The ISO setting when the photo was taken | String | No | Yes |
| photo-orientation | The camera orientation when the photo was taken | String | No | Yes |
| photo-resolution-x | The width of the image, in pixels | Integer | No | Yes |
| photo-resolution-y | The height of the image, in pixels | Integer | No | Yes |

### Bulk Import default mapping column

By using the Default Column Names in a CSV for a Bulk Import, Camelot will automatically establish the mapping to the correct field.

| Field | Default Column Name |
| :---- | :------------------ |
| camera-make | Make |
| camera-model | Model |
| camera-name | Camera Name |
| camera-notes | Camera Notes |
| media-attention-needed | Attention Needed Flag |
| media-cameracheck | Camera Check Flag |
| media-capture-timestamp | Date/Time |
| media-processed | Media Processed Flag |
| photo-exposure-value | Exposure Bias Value |
| photo-flash-setting | Flash |
| photo-fnumber-setting | Aperture Value |
| photo-focal-length | Focal Length |
| photo-iso-setting | ISO Speed Ratings |
| photo-orientation | Orientation |
| photo-resolution-x | Image Height |
| photo-resolution-y | Image Width |
| sighting-lifestage | Sighting Life Stage |
| sighting-quantity | Sighting Quantity |
| sighting-sex | Sighting Sex |
| site-area | Site Area (km2) |
| site-city | City |
| site-country | Country/Primary Location Name |
| site-name | Site Name |
| site-notes | Site Notes |
| site-state-province | Province/State |
| site-sublocation | Sub-location |
| taxonomy-class | Class |
| taxonomy-common-name | Species Common Name |
| taxonomy-family | Family |
| taxonomy-genus | Genus |
| taxonomy-notes | Species Notes |
| taxonomy-order | Order |
| taxonomy-species | Species |
| trap-station-altitude | GPS Altitude |
| trap-station-latitude | Camelot GPS Latitude |
| trap-station-longitude | Camelot GPS Longitude |
| trap-station-name | Trap Station Name |
| trap-station-notes | Trap Station Notes |
| trap-station-session-end-date | Session End Date |
| trap-station-session-start-date | Session Start Date |

### Library filter field shorthands

The library supports filtering on the vast majority of fields in Camelot.  Some of these fields are more useful for filtering on than others and so have shorthands.  Below is each of the shorthands and the field they are associated with.

| Shorthand | Associated Field |
| :---- | :------------------ |
| camera | camera-name |
| city | site-city |
| class | taxonomy-class |
| common | taxonomy-common-name |
| family | taxonomy-family |
| flagged | media-attention-needed |
| genus | taxonomy-genus |
| lat | trap-station-latitude |
| loc | site-sublocation |
| long | trap-station-longitude |
| make | camera-make |
| model | camera-model |
| order | taxonomy-order |
| processed | media-processed |
| reference-quality | media-reference-quality |
| site | site-name |
| species | taxonomy-label |
| testfire | media-cameracheck |
| trap | trap-station-name |
| trapid | trap-station-id |
