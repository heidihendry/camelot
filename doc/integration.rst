Integrating Camelot
-------------------

Camelot is designed with integration in mind. This page describes the process around some common integrations.

CamtrapR
~~~~~~~~

CamtrapR assumes images to be stored in certain ways in order to read-in data for analysing.  Camelot itself does not store data in this way.

Instead, Camelot has two CamtrapR-specific reports:

1. Camera Trap Export
2. Record Table

These reports provide data in an equivalent format to CamtrapR's ``recordTable`` and ``CTtable``

Here's an example of how these might be used in R to generate a species richness plot:

.. code:: R

  recordtable = read.csv("record-table_yyyy-mm-dd_xxxx.csv") # read the Record Table report
  camtraps = read.csv("camera-traps_yyyy-mm-dd_xxxx.csv") # read the Camera Trap Export report

  Maps <- detectionMaps(CTtable      = camtraps,
                        recordTable  = recordtable,
                        Xcol         = "gps_x",
                        Ycol         = "gps_y",
                        stationCol   = "Station",
                        speciesCol   = "Species",
                        printLabels  = TRUE,
                        richnessPlot = TRUE,
                        speciesPlots = FALSE
  )
