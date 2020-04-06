Animal detection
------------------

*The features described on this page are coming in Camelot 1.6.0.*

Typically most images captured by motion-triggered cameras are do not have
valuable content, as the motion sensor was triggered by causes other than some
nearby animal, such as a gust of wind. The result is that much time tends to
be spent sifting through images to find those that actually have interesting
content.

To help you focus on the images that matter most, Camelot offers animal
detection. In practice, this enables two features:

1. the ability to filter images in the library based on whether they have
   animals or not, even before they have been processed

.. figure:: screenshot/has-wildlife.png
   :alt: 

2. bounding boxes around the detected animals

.. figure:: screenshot/suggestion.png
   :alt: 

Setup
~~~~~

Animal detection is provided via an online service. As such, its use requires
an adequate internet connection and it is only available after registration.

For most users, set up is a simple matter of following the prompts in the
|administration_ui|:

.. figure:: screenshot/detection-settings.png
   :alt: 

.. |administration_ui| raw:: html

   <a href="administration.html">administration interface</a>

Once login has succeeded, save the configuration, restart Camelot, and the
animal detection should now start to upload & process the available images.

How it works
~~~~~~~~~~~~

Once set up, the animal detection automatically starts, and will continue
behind-the-scenes. You can see a summary of this detection activity at any
time via the top "Detection" menu:

.. figure:: screenshot/detection-activity.png
   :alt: 

Detection is performed by following the steps described below.

The detection starts by retrieving the cameras for each session of all trap
stations.  A batch of images is formed for each of these cameras, which is
referred to as a 'image batch creation'.

Images will be uploaded to secure cloud storage location dedicated to this
image batch.  When all images have been uploaded, this batch is submitted for
processing.

Camelot has partnered with Microsoft's |ai_for_earth| team, who at this stage,
perform the image processing using their machine learning models. Processing
can take some time, typically between several minutes and several hours,
depending on the number of images in the batch.  Camelot will periodically
check for new results for the submitted batches.

Once processed, the suggestions offered by the image processing are created in
Camelot.  "High confidence" suggestions are reflected in the library in the
image filtering, and with bounding boxes.

Whether a suggestion is considered "high-confidence" is determined by the
"Confidence threshold" setting in the Administration UI, which is a number
between ``0`` and ``1`` indicating the likelihood of the suggestion being correct.
Suggestions made at or beyond this level of confidence are "high-confidence".

The threshold can be changed at any time. All suggestions are stored by
Camelot regardless of the confidence threshold, so changes to the threshold
apply retrospectively.

.. |ai_for_earth| raw:: html

   <a href="https://www.microsoft.com/en-us/ai/ai-for-earth">AI for Earth</a>

Filtering
~~~~~~~~~

Suggestions created by animal detection include whether the an ``animal`` or
``person`` was detected. The "Has animal?" filter in the Library shows only
those images that have a high-confidence ``animal`` suggestion, or has an
identification.

Images with high-confidence suggestions that they contain people can be shown
using the filter:

``suggestion-key:person``

Statuses
~~~~~~~~

The current detector status is reflected to the right of the "Activity"
section.  The detector can be paused, running or in an offline status.

The detector can be paused at any time. This can be particularly useful for
slower connections, where the network bandwidth required to upload images may
noticably degrade the connection. In this event, the detector can be paused
which will prevent any new uploads taking place until it is resumed again.

While paused, the detector will take no further action, including creating
suggestions from any newly-processed images. All such actions will be queued
until the detector is resumed again.

When toggling between running and paused it may take several seconds before
Camelot reflects the new status. This is normal: the status is only reflected
once it is actually processed, which means the current activities (e.g.,
upload of the current image) will need to complete before the new status is in
effect.

Camelot may signal here that the authentication failed if the configured
username and password are rejected.  In this event, the detector is
effectively offline and Camelot will need to be restarted before the detector
will attempt to recheck the credentials and run again.

Connectivity detection
^^^^^^^^^^^^^^^^^^^^^^

What we would not want is for the internet to go down for an hour or two, and
find a large number of batches and image uploads have failed as a result.

In the event Camelot cannot access the online services it needs, it will pause
the processing automatically. Once the connection is restored, processing will
be automatically resumed.

If the system is paused through the user interface, Camelot will respect this
even if the internet connection cuts out and comes back. Camelot will always
pause the animal detection system in the event it cannot communicate with the
systems it needs to.

Activity
~~~~~~~~

Camelot tracks and aggregates all animal detection activity, presenting it on
the activity page described above. This gives an overview of what is happening
within Camelot, and provides visibility in to any errors which may be
occurring.

This page reports failures and suspended tasks.  A failure is a step which
cannot be completed, whereas a a step which has been suspend will be retried
again after other batches have been processed.

Failures and suspensions happen for a variety of reasons, including network
disruptions or delays from processing particularly large batches.  Some errors
may mean that suggestions for a small number of images are not created where
they otherwise could have been, though typically these are not worth worrying
about; false negatives can be assumed to exist in the suggestions anyway, and
thus you should treat failures as potential false-negative.
